package tw.com.topbs.system.service.impl;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.system.exception.SysChunkFileException;
import tw.com.topbs.system.mapper.SysChunkFileMapper;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
import tw.com.topbs.system.pojo.VO.CheckFileVO;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.pojo.entity.SysChunkFile;
import tw.com.topbs.system.service.SysChunkFileService;
import tw.com.topbs.utils.S3Util;

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

	// 使用自定義配置的線程池
	@Qualifier("taskExecutor")
	private final Executor taskExecutor;

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	// 預設儲存桶名稱
	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	private final S3Util s3Util;

	// Redisson Keys 儲存 uploadId, totalChunks
	private static final String S3_META_KEY_PREFIX = "s3:meta:";
	// Redisson Keys 儲存 Map<partNumber, eTag>
	private static final String S3_PARTS_KEY_PREFIX = "s3:parts:";
	// redis 分片過期時間 8小時,過長會導致過期的分片無法刪除
	private static final Integer CACHE_EXPIRE_HOURS = 8;

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
	public ChunkResponseVO uploadChunkS3(MultipartFile file, String mergedBasePath,
			@Valid ChunkUploadDTO chunkUploadDTO) {
		final String sha256 = chunkUploadDTO.getFileSha256();
		final int totalChunks = chunkUploadDTO.getTotalChunks();
		// S3 Part Number 從 1 開始
		final int partNumber = chunkUploadDTO.getChunkIndex() + 1;

		// 檢查 S3 合併限制 (雖然 S3 上限更高，但保持您原有的防禦性檢查)
		if (totalChunks > 10000) { // S3 支援最高 10000 個分片
			throw new SysChunkFileException("分片超過10000片，不符合 S3 協議上限。");
		}

		// Keys for Redisson
		String metaKey = S3_META_KEY_PREFIX + sha256;
		String partsKey = S3_PARTS_KEY_PREFIX + sha256;

		// 獲取狀態 Map<String, Object> (MinIO 模式中的 metaMap，現在用於儲存 uploadId)
		RMap<String, Object> metaMap = redissonClient.getMap(metaKey);
		// 獲取分片 Map<Integer, String> (Key=partNumber, Value=eTag)
		RMap<Integer, String> uploadedPartsMap = redissonClient.getMap(partsKey);

		String uploadId = (String) metaMap.get("uploadId");

		// 組成S3 Key
		String s3Key = s3Util.normalizePath(mergedBasePath) + chunkUploadDTO.getFileName();

		// 1. 斷點續傳檢查
		if (uploadedPartsMap.containsKey(partNumber)) {
			System.out.println("分片 " + partNumber + " 已存在SHA256 : " + sha256 + " 跳過上傳");
			log.warn("分片 {} 已存在，跳過上傳。SHA256={}", partNumber, sha256);
			int currentUploadedCount = uploadedPartsMap.size();
			return new ChunkResponseVO(currentUploadedCount, totalChunks, chunkUploadDTO.getChunkIndex(), sha256, null);
		}

		// 2. 初始化 S3 Multipart Upload (僅在處理第一個分片且未初始化時執行)
		if (uploadId == null) {
			String metaLockKey = "meta-lock:" + sha256;
			RLock metaLock = redissonClient.getLock(metaLockKey);
			boolean locked = false;

			try {
				locked = metaLock.tryLock(5, 10, TimeUnit.SECONDS);
				if (locked) {
					uploadId = (String) metaMap.get("uploadId"); // 鎖內 double check
					if (uploadId == null) {
						System.out.println("開始初始化");
						// 呼叫 S3Util 初始化
						uploadId = s3Util.initializeMultipartUpload(s3Key, chunkUploadDTO.getFileType(),
								Map.of("sha256", sha256));

						System.out.println("初始化成功");

						// 儲存初始化資訊
						metaMap.put("uploadId", uploadId);
						metaMap.put("totalChunks", totalChunks);
						metaMap.put("fileName", chunkUploadDTO.getFileName());
						metaMap.put("s3Key", s3Key);
						metaMap.expire(Duration.ofHours(CACHE_EXPIRE_HOURS));
						uploadedPartsMap.expire(Duration.ofHours(CACHE_EXPIRE_HOURS));

						// 建立資料庫記錄（確保唯一性）
						SysChunkFile exist = baseMapper.selectOne(new LambdaQueryWrapper<SysChunkFile>()
								.eq(SysChunkFile::getFileSha256, chunkUploadDTO.getFileSha256()));
						if (exist == null) {
							SysChunkFile sysChunkFile = new SysChunkFile();
							//sysChunkFile.setFileId(UUID.randomUUID().toString());
							sysChunkFile.setFileId(uploadId);
							sysChunkFile.setFileSha256(chunkUploadDTO.getFileSha256());
							sysChunkFile.setFileName(chunkUploadDTO.getFileName());
							sysChunkFile.setFileType(chunkUploadDTO.getFileType());
							sysChunkFile.setStatus(0);
							sysChunkFile.setTotalChunks(chunkUploadDTO.getTotalChunks());
							sysChunkFile.setUploadedChunks(uploadedPartsMap.size());
							baseMapper.insert(sysChunkFile);
						}
					}
				}
			} catch (Exception e) {
				log.error("S3 初始化時獲取鎖失敗: {}", sha256, e);
				// 註: 這裡可以嘗試 Abort 剛剛初始化的 uploadId，但為簡潔暫不實現
				throw new RuntimeException("S3 初始化鎖失敗或初始化異常", e);
			} finally {
				if (locked)
					metaLock.unlock();
			}
		}

		// 3. 上傳分片
		try {

			System.out.println(partNumber + "分片開始上傳");

			// 呼叫 S3Util 上傳分片
			String eTag = s3Util.uploadPart(s3Key, uploadId, partNumber, file);

			// 記錄 ETag 到 Redis
			uploadedPartsMap.put(partNumber, eTag);

		} catch (Exception e) {
			log.error("分片上傳失敗: part={}, sha256={}", partNumber, sha256, e);
			// 此處不 Abort，允許前端重試
			throw new RuntimeException("分片上傳 S3 失敗", e);
		}

		// 4. 從Redis 更新最新數量到程式中
		int currentUploadedCount = uploadedPartsMap.size();

		// 5. 檢查是否完成，並觸發合併
		if (currentUploadedCount == totalChunks) {
			String finalPath = null;

			// 避免競態條件鎖 (保持 MinIO 邏輯)
			String lockKey = "merge-lock:" + sha256;
			RLock lock = redissonClient.getLock(lockKey);
			boolean isLock = false;

			try {
				isLock = lock.tryLock(10, 300, TimeUnit.SECONDS);
				if (isLock) {
					// Double check：確認 Redis 狀態是否仍完整
					if (uploadedPartsMap.size() == totalChunks) {
						System.out.println("所有分片上傳完畢，觸發 S3 合併: " + sha256);
						log.info("所有分片上傳完畢，觸發 S3 合併: {}", sha256);

						// 呼叫 S3 合併邏輯
						finalPath = this.completeS3MultipartUpload(sha256, uploadId, s3Key, totalChunks,
								uploadedPartsMap, mergedBasePath);
					} else {
						//  uploadedPartsMap 已被清空 ，檢查 merge-result
						RBucket<String> mergeResult = redissonClient.getBucket("merge-result:" + uploadId);
						String existingFinalPath = mergeResult.get();
						if (existingFinalPath != null) {
							return new ChunkResponseVO(currentUploadedCount, totalChunks,
									chunkUploadDTO.getChunkIndex(), sha256, existingFinalPath);
						}
					}
				}
			} catch (Exception e) {
				log.error("S3 合併失敗: {}", sha256, e);
			} finally {
				if (isLock)
					lock.unlock();
			}

			// 如果 finalPath 不為 null，則合併成功，可以直接返回完成狀態
			if (finalPath != null) {
				// 返回完成狀態
				return new ChunkResponseVO(currentUploadedCount, totalChunks, chunkUploadDTO.getChunkIndex(), sha256,
						finalPath);
			}
		}

		// 6. 最後回傳一個當前進度
		return new ChunkResponseVO(currentUploadedCount, totalChunks, chunkUploadDTO.getChunkIndex(), sha256, null);
	}

	/**
	 * 輔助方法：分片合併
	 * 
	 * @param sha256
	 * @param uploadId
	 * @param s3Key
	 * @param totalChunks
	 * @param uploadedPartsMap
	 * @param mergedBasePath
	 * @return
	 */
	private String completeS3MultipartUpload(String sha256, String uploadId, String s3Key, int totalChunks,
			RMap<Integer, String> uploadedPartsMap, String mergedBasePath) {

		SysChunkFile sysChunkFile = null;
		String finalUrl = null;

		try {
			// 1. 準備 PartInfo 列表
			// S3 合併要求 PartInfo 必須按 PartNumber 升序排列
			List<S3Util.PartInfo> parts = uploadedPartsMap.entrySet()
					.stream()
					.map(e -> new S3Util.PartInfo(e.getKey(), e.getValue()))
					.sorted(Comparator.comparingInt(S3Util.PartInfo::getPartNumber))
					.collect(Collectors.toList());

			// 2. 獲取 DB 紀錄 
			sysChunkFile = baseMapper
					.selectOne(new LambdaQueryWrapper<SysChunkFile>().eq(SysChunkFile::getFileSha256, sha256));
			if (sysChunkFile == null) {
				throw new SysChunkFileException("DB record missing: Cannot proceed with merge.");
			}

			// 3. 呼叫 S3Util 完成合併
			// 註：S3 合併不會改變 Key，所以 s3Key 就是最終路徑
			finalUrl = s3Util.completeMultipartUpload(s3Key, uploadId, parts);

			// 4. 更新資料庫
			sysChunkFile.setFilePath(s3Key); // S3 Key 就是路徑
			sysChunkFile.setUploadedChunks(totalChunks);
			sysChunkFile.setStatus(1);
			//S3 合併後要獲取 FileSize 
			long fileSize = s3Util.getFileSize(s3Key);
			sysChunkFile.setFileSize(fileSize);
			baseMapper.updateById(sysChunkFile);

			log.info("S3 合併完成: sha256={}, finalUrl={}", sha256, finalUrl);
			// --- 合併成功後，寫入 merge-result ---
			// 取得 RBucket<String>，key 為合併結果 + uploadId , value 為儲存的地址 ， 保留五分鐘
			RBucket<String> mergeResult = redissonClient.getBucket("merge-result:" + uploadId);
			mergeResult.set(finalUrl, 5, TimeUnit.MINUTES);
			return finalUrl;

		} catch (Exception e) {
			// 合併失敗，必須取消上傳以避免費用和髒數據
			s3Util.abortMultipartUpload(s3Key, uploadId);

			if (sysChunkFile != null) {
				sysChunkFile.setUploadedChunks(totalChunks);
				sysChunkFile.setStatus(99);
				baseMapper.updateById(sysChunkFile);
			}
			log.error("S3 分片合併錯誤，已取消上傳: {}", sha256, e);
			throw new SysChunkFileException("S3 分片合併失敗，已取消上傳。");
		} finally {
			// 5. 清理 Redis 緩存
			uploadedPartsMap.delete();
			redissonClient.getMap(S3_META_KEY_PREFIX + sha256).delete();

			// 6. S3 Multipart Upload 不需要刪除 chunks，因為 S3 合併後會自動清理分片
			// MinIO 的 composeObject 是不同的機制，所以 MinIO 寫法需要手動刪除 chunks。
			// S3 的 completeMultipartUpload 完成後，中間分片會被 S3 服務器清理。
		}
	}

	@Override
	public void deleteSysChunkFileByPath(String minioPath) {
		LambdaQueryWrapper<SysChunkFile> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SysChunkFile::getFilePath, minioPath);

		baseMapper.delete(queryWrapper);

	}

}
