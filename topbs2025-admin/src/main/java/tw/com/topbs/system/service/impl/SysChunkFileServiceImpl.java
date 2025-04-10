package tw.com.topbs.system.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.minio.ComposeObjectArgs;
import io.minio.ComposeSource;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.system.exception.SysChunkFileException;
import tw.com.topbs.system.mapper.SysChunkFileMapper;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
import tw.com.topbs.system.pojo.VO.CheckFileVO;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.pojo.entity.SysChunkFile;
import tw.com.topbs.system.service.SysChunkFileService;
import tw.com.topbs.utils.MinioUtil;

/**
 * <p>
 * 系統通用，大檔案分片上傳，5MB以上就可處理，這邊僅記錄這個大檔案的上傳進度 和 狀況，
 * 合併後的檔案在minio，真實的分片區塊，會放在臨時資料夾，儲存資料會在redis 服務實現類
 * </p>
 *
 * @author Joey
 * @since 2025-04-07
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class SysChunkFileServiceImpl extends ServiceImpl<SysChunkFileMapper, SysChunkFile>
		implements SysChunkFileService {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private final MinioUtil minioUtil;

	// MinioClient对象，用于与MinIO服务进行交互
	private final MinioClient minioClient;

	@Qualifier("taskExecutor") // 使用您配置的線程池
	private final Executor taskExecutor;

	// 預設存储桶名称
	@Value("${minio.bucketName}")
	private String bucketName;

	// Redis中Set， chunk index集合，key前墜，用於蒐集以上傳的分塊
	private static final String CHUNK_KEY_SET_PREFIX = "chunk:uploaded:";
	// Redis中Map， 放 totalChunks、fileName、過期時間的元數據
	private static final String META_KEY_PREFIX = "chunk:meta::";
	// redis 分片過期時間
	private static final Integer CACHE_EXPIRE_HOURS = 72;

	@Override
	public CheckFileVO checkFile(String sha256) {

		// 透過SHA256值，查找資料庫有沒有這筆資料
		LambdaQueryWrapper<SysChunkFile> sysChunkFileWrapper = new LambdaQueryWrapper<>();
		sysChunkFileWrapper.eq(SysChunkFile::getFileSha256, sha256);
		SysChunkFile sysChunkFile = baseMapper.selectOne(sysChunkFileWrapper);

		CheckFileVO checkFileVO = new CheckFileVO();

		// 如果檔案已經存在 且 已經合併完成，則返回 已存在 和 檔案路徑
		if (sysChunkFile != null && sysChunkFile.getStatus() == 1) {
			checkFileVO.setExist(true);
			checkFileVO.setPath(sysChunkFile.getFilePath());
			return checkFileVO;
		} else {
			// 沒有則直接返回不存在，路徑則為null
			checkFileVO.setExist(false);
			return checkFileVO;
		}

	}

	@Override
	@Transactional
	public ChunkResponseVO uploadChunk(MultipartFile file, ChunkUploadDTO chunkUploadDTO) {

		String chunkKey = CHUNK_KEY_SET_PREFIX + chunkUploadDTO.getFileSha256();
		String metaKey = META_KEY_PREFIX + chunkUploadDTO.getFileSha256();

		// 先判斷分片不可以超過1000片，因為S3合併協議的關係
		if (chunkUploadDTO.getTotalChunks() > 1000) {
			throw new SysChunkFileException("分片超過1000片，不符合S3協議的合併物件");
		}

		// 根據key 獲得Redis中的Set物件，沒有就會創建一個
		RSet<Integer> uploadedChunks = redissonClient.getSet(chunkKey);

		// 獲取redis中關於這個分片檔案的Map資訊
		RMap<String, Object> metaMap = redissonClient.getMap(metaKey);

		try {

			// 支援斷點續傳，當這個chunk Redis已經有這個檔案分片時，直接返回，不用重新上傳一次
			if (uploadedChunks.contains(chunkUploadDTO.getChunkIndex())) {

				// 這個Chunk已經收集過了，直接回傳一個當前進度
				return new ChunkResponseVO(uploadedChunks.size(), chunkUploadDTO.getTotalChunks(),
						chunkUploadDTO.getChunkIndex(), chunkUploadDTO.getFileSha256());
			}

			// 創建臨時檔案(分片)，最後會直接在minio中進行合併
			String chunkObject = "chunks/" + chunkUploadDTO.getFileSha256() + "_" + chunkUploadDTO.getChunkIndex();

			// 上傳分片至 MinIO
			minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(chunkObject)
					.stream(file.getInputStream(), file.getSize(), -1).contentType(chunkUploadDTO.getFileType())
					.build());

			// 第一次快取判斷（快，不上鎖）
			if (!metaMap.containsKey("totalChunks")) {

				// 僅當需要初始化時才嘗試搶鎖
				String metaLockKey = "meta-lock:" + chunkUploadDTO.getFileSha256();
				RLock metaLock = redissonClient.getLock(metaLockKey);

				boolean locked = false;
				try {
					locked = metaLock.tryLock(5, 10, TimeUnit.SECONDS);
					if (locked) {
						// 第二次確認（鎖內再次確認，避免競態）
						if (!metaMap.containsKey("totalChunks")) {
							metaMap.put("totalChunks", chunkUploadDTO.getTotalChunks());
							metaMap.put("fileName", chunkUploadDTO.getFileName());
							metaMap.expire(Duration.ofHours(CACHE_EXPIRE_HOURS));

							// 建立資料庫記錄（確保唯一性）
							SysChunkFile exist = baseMapper.selectOne(new LambdaQueryWrapper<SysChunkFile>()
									.eq(SysChunkFile::getFileSha256, chunkUploadDTO.getFileSha256()));
							if (exist == null) {
								SysChunkFile sysChunkFile = new SysChunkFile();
								sysChunkFile.setFileId(UUID.randomUUID().toString());
								sysChunkFile.setFileSha256(chunkUploadDTO.getFileSha256());
								sysChunkFile.setFileName(chunkUploadDTO.getFileName());
								sysChunkFile.setFileType(chunkUploadDTO.getFileType());
								sysChunkFile.setStatus(0);
								sysChunkFile.setTotalChunks(chunkUploadDTO.getTotalChunks());

								sysChunkFile.setUploadedChunks(uploadedChunks.size());
								baseMapper.insert(sysChunkFile);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					log.error("metaMap 初始化時獲取鎖失敗: {}", chunkUploadDTO.getFileSha256(), e);
				} finally {
					if (locked) {
						metaLock.unlock();
					}
				}
			}

			// Redis中記錄此分片已上傳
			uploadedChunks.add(chunkUploadDTO.getChunkIndex());
			uploadedChunks.expire(Duration.ofHours(CACHE_EXPIRE_HOURS));

			// 更新已上傳數量
			SysChunkFile updatingFile = baseMapper.selectOne(new LambdaQueryWrapper<SysChunkFile>()
					.eq(SysChunkFile::getFileSha256, chunkUploadDTO.getFileSha256()));

			if (updatingFile != null) {
				updatingFile.setUploadedChunks(uploadedChunks.size());
				baseMapper.updateById(updatingFile);
			}

			// 如果所有Chunk 在 Redis 中已獲得（若所有分片已上傳），嘗試合併
			if (uploadedChunks.size() == chunkUploadDTO.getTotalChunks()) {

				System.out.println("所有分片上傳完畢，觸發合併");

				// 避免競態條件，多個使用者同時上傳分片，最後一塊可能會同時觸發自動合併邏輯。
				// 針對這個檔案重複合併，做一個鎖
				String lockKey = "merge-lock:" + chunkUploadDTO.getFileSha256();
				RLock lock = redissonClient.getLock(lockKey);

				boolean isLock = false;
				try {
					// 最多等10秒，得到鎖300秒後自動過期，也就是自動過期
					isLock = lock.tryLock(10, 300, TimeUnit.SECONDS);
					// 當搶到這把鎖
					if (isLock) {
						/**
						 * 
						 * Double check，確保沒有其他人先合併過，避免競態條件
						 * 1.兩個用戶或線程同時上傳最後一個 chunk。
						 * 2.兩邊同時判斷 uploadedChunks.size() == totalChunks 為 true。
						 * 3.都進入 tryLock 嘗試合併，只有一邊會搶到鎖。
						 * 4.搶到鎖的人執行 mergeChunks()，同時刪除 Redis 資訊。
						 * 5.沒搶到鎖的線程過了幾秒才拿到鎖，這時 Redis 的 chunk set 其實已經被刪掉或清空。
						 * 
						 */
						RSet<Integer> checkUploaded = redissonClient
								.getSet(CHUNK_KEY_SET_PREFIX + chunkUploadDTO.getFileSha256());
						if (checkUploaded.size() == chunkUploadDTO.getTotalChunks()) {
							log.info("All chunks uploaded, triggering auto-merge: {}", chunkUploadDTO.getFileSha256());
							this.mergeChunks(chunkUploadDTO.getFileSha256(), chunkUploadDTO.getFileName(),
									chunkUploadDTO.getTotalChunks());
						}
					}
				} catch (Exception e) {
					log.error("Auto-merge failed for {}", chunkUploadDTO.getFileSha256(), e);
				} finally {
					// 最終要記得解鎖
					if (isLock) {
						lock.unlock();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error("分片上傳發生異常", e);
		}

		// 最後回傳一個當前進度
		return new ChunkResponseVO(uploadedChunks.size(), chunkUploadDTO.getTotalChunks(),
				chunkUploadDTO.getChunkIndex(), chunkUploadDTO.getFileSha256());

	}

	@Override
	public Map<String, String> mergeChunks(String sha256, String fileName, Integer totalChunks) {

		String chunkKey = CHUNK_KEY_SET_PREFIX + sha256;
		String metaKey = META_KEY_PREFIX + sha256;

		try {
			RSet<Integer> uploadedSet = redissonClient.getSet(chunkKey);
			if (uploadedSet.size() < totalChunks) {
				System.out.println("Not all chunks have been uploaded");
				return null;
			}

			// 準備合併目標路徑
			String fileId = UUID.randomUUID().toString();
			String fileExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
			String mergedFilePath = "merged/" + fileId + fileExt;

			// 建立 ComposeSources
			System.out.println("建立物件合併列表");
			List<ComposeSource> sources = IntStream.range(0, totalChunks).mapToObj(
					i -> ComposeSource.builder().bucket(bucketName).object("chunks/" + sha256 + "_" + i).build())
					.collect(Collectors.toList());

			// 合併
			/**
			 * 
			 * 限制說明（依據 MinIO / S3 規範）
			 * 最多只能合併 1000 個物件：
			 * composeObject() 支援的最大來源物件數為 1000 個（這是因為它是基於 S3 的 Multipart Upload -
			 * CompleteMultipartUpload 規格）。
			 * 
			 * 單個來源物件的最小大小為 5 MiB（除非是最後一個）：
			 * 除了最後一個來源物件外，每個來源物件的大小必須大於等於 5 MiB，否則會出錯（和 S3 multipart upload 的規定一樣）。
			 * 
			 * 目標物件大小限制：最大 5 TiB：
			 * 合併後的最終物件大小不能超過 5 TiB（S3 上限）。
			 * 
			 */
			System.out.println("開始合併");
			minioClient.composeObject(
					ComposeObjectArgs.builder().bucket(bucketName).object(mergedFilePath).sources(sources).build());

			// 取檔案資訊
			StatObjectResponse stat = minioClient
					.statObject(StatObjectArgs.builder().bucket(bucketName).object(mergedFilePath).build());

			// 資料庫找到這筆資料並更新
			System.out.println("資料庫更新資料");

			SysChunkFile sysChunkFile = baseMapper
					.selectOne(new LambdaQueryWrapper<SysChunkFile>().eq(SysChunkFile::getFileSha256, sha256));
			sysChunkFile.setFilePath(mergedFilePath);
			sysChunkFile.setUploadedChunks(totalChunks);
			sysChunkFile.setStatus(1);
			sysChunkFile.setFileSize(stat.size());

			baseMapper.updateById(sysChunkFile);

			// 清除 Redis 緩存
			System.out.println("清除redis緩存");
			uploadedSet.delete();
			redissonClient.getMap(metaKey).delete();

			// 刪除每個 chunk
			for (int i = 0; i < totalChunks; i++) {
				try {
					minioClient.removeObject(
							RemoveObjectArgs.builder().bucket(bucketName).object("chunks/" + sha256 + "_" + i).build());
				} catch (Exception ex) {
					log.warn("Failed to delete chunk: {}_{}", sha256, i);
				}
			}

			System.out.println("合併完成");
			return Map.of("fileId", fileId, "filePath", mergedFilePath);
		} catch (Exception e) {

			e.printStackTrace();
			log.error("分片合併錯誤", e);
			throw new SysChunkFileException("Large file upload. Part merging failed.");
		}

	}

	@Override
	public void downloadFile(String fileId, HttpServletResponse response) {
		// TODO Auto-generated method stub

	}

}
