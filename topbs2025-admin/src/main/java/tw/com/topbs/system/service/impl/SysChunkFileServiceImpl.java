package tw.com.topbs.system.service.impl;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	// MinioClient对象，用于与MinIO服务进行交互
	private final MinioClient minioClient;
	private final S3Util s3Util;

	@Qualifier("taskExecutor") // 使用您配置的線程池
	private final Executor taskExecutor;

	// 預設存储桶名称
	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	// Redisson Keys
	private static final String S3_META_KEY_PREFIX = "s3:meta:"; // 儲存 uploadId, totalChunks
	private static final String S3_PARTS_KEY_PREFIX = "s3:parts:"; // 儲存 Map<partNumber, eTag>

	// Redis中Set， chunk index集合，key前墜，用於蒐集以上傳的分塊
	private static final String CHUNK_KEY_SET_PREFIX = "chunk:uploaded:";
	// Redis中Map， 放 totalChunks、fileName、過期時間的元數據
	private static final String META_KEY_PREFIX = "chunk:meta::";
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
		String s3Key = "uploads/" + sha256 + "/" + chunkUploadDTO.getFileName(); // 臨時 S3 Key

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

						// 建立資料庫記錄 (保持 MinIO 邏輯)
						// ... (省略 DB 插入邏輯，與您的 MinIO 寫法一致) ...
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

			System.out.println("其餘分片開始上傳");

			// 呼叫 S3Util 上傳分片
			String eTag = s3Util.uploadPart(s3Key, uploadId, partNumber, file);

			// 記錄 ETag 到 Redis
			uploadedPartsMap.put(partNumber, eTag);

		} catch (Exception e) {
			log.error("分片上傳失敗: part={}, sha256={}", partNumber, sha256, e);
			// 此處不 Abort，允許前端重試
			throw new RuntimeException("分片上傳 S3 失敗", e);
		}

		// 4. 更新已上傳數量到 DB (保持 MinIO 邏輯)
		int currentUploadedCount = uploadedPartsMap.size();
		// ... (省略 DB 更新邏輯，與您的 MinIO 寫法一致) ...

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
						// 如果不是最後一片，或者 uploadedPartsMap 已被清空
						// 可以檢查 merge-result
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

	// ===============================================
	// 輔助方法：取代 MinIO 的 mergeChunks 邏輯
	// ===============================================

	private String completeS3MultipartUpload(String sha256, String uploadId, String s3Key, int totalChunks,
			RMap<Integer, String> uploadedPartsMap, String mergedBasePath) {

//		SysChunkFile sysChunkFile = null;
		String finalUrl = null;

		try {
			// 1. 準備 PartInfo 列表
			// S3 合併要求 PartInfo 必須按 PartNumber 升序排列
			List<S3Util.PartInfo> parts = uploadedPartsMap.entrySet()
					.stream()
					.map(e -> new S3Util.PartInfo(e.getKey(), e.getValue()))
					.sorted(Comparator.comparingInt(S3Util.PartInfo::getPartNumber))
					.collect(Collectors.toList());

			// 2. 獲取 DB 紀錄 (保持 MinIO 邏輯)
//			sysChunkFile = baseMapper
//					.selectOne(new LambdaQueryWrapper<SysChunkFile>().eq(SysChunkFile::getFileSha256, sha256));
//			if (sysChunkFile == null) {
//				throw new SysChunkFileException("DB record missing: Cannot proceed with merge.");
//			}

			// 3. 呼叫 S3Util 完成合併
			// 註：S3 合併不會改變 Key，所以 s3Key 就是最終路徑
			finalUrl = s3Util.completeMultipartUpload(s3Key, uploadId, parts);

			// 4. 更新資料庫
//			sysChunkFile.setFilePath(s3Key); // S3 Key 就是路徑
//			sysChunkFile.setUploadedChunks(totalChunks);
//			sysChunkFile.setStatus(1);
			// 註：S3 合併後要獲取 FileSize 需要額外調用 StatObject，為簡潔省略
			// sysChunkFile.setFileSize(stat.size()); 
			//			baseMapper.updateById(sysChunkFile);

			log.info("S3 合併完成: sha256={}, finalUrl={}", sha256, finalUrl);
			// --- 合併成功後，寫入 merge-result ---
			// 取得 RBucket<String>，key 為合併結果 + uploadId , value 為儲存的地址
			RBucket<String> mergeResult = redissonClient.getBucket("merge-result:" + uploadId);
			mergeResult.set(finalUrl, 5, TimeUnit.MINUTES);
			return finalUrl;

		} catch (Exception e) {
			// 合併失敗，必須取消上傳以避免費用和髒數據
			s3Util.abortMultipartUpload(s3Key, uploadId);

//			if (sysChunkFile != null) {
//				sysChunkFile.setUploadedChunks(totalChunks);
//				sysChunkFile.setStatus(99);
//				//				baseMapper.updateById(sysChunkFile);
//			}
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
	public ChunkResponseVO uploadChunk(MultipartFile file, String mergedBasePath,
			@Valid ChunkUploadDTO chunkUploadDTO) {
		String chunkKey = CHUNK_KEY_SET_PREFIX + chunkUploadDTO.getFileSha256();
		String metaKey = META_KEY_PREFIX + chunkUploadDTO.getFileSha256();
		// ✅ 在方法層級宣告，預設值為 0
		int currentUploadedCount = 0;

		// 初始化最終merged後的儲存路徑
		String filePath = null;

		// 先判斷分片不可以超過1000片，因為S3合併協議的關係
		if (chunkUploadDTO.getTotalChunks() > 1000) {
			throw new SysChunkFileException("分片超過1000片，不符合S3協議的合併物件");
		}

		// 根據key 獲得Redis中的Set物件，沒有就會創建一個
		RSet<Integer> uploadedChunks = redissonClient.getSet(chunkKey);

		// 獲取redis中關於這個分片檔案的Map資訊
		RMap<String, Object> metaMap = redissonClient.getMap(metaKey);

		System.out.println("當前處理Index:" + chunkUploadDTO.getChunkIndex());

		try {

			// 支援斷點續傳，當這個chunk Redis已經有這個檔案分片時，直接返回，不用重新上傳一次
			if (uploadedChunks.contains(chunkUploadDTO.getChunkIndex())) {

				// 這個Chunk已經收集過了，直接回傳一個當前進度
				return new ChunkResponseVO(uploadedChunks.size(), chunkUploadDTO.getTotalChunks(),
						chunkUploadDTO.getChunkIndex(), chunkUploadDTO.getFileSha256(), filePath);
			}

			// 創建臨時檔案(分片)，最後會直接在minio中進行合併
			String chunkObject = "chunks/" + chunkUploadDTO.getFileSha256() + "_" + chunkUploadDTO.getChunkIndex();

			// 上傳分片至 MinIO
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(bucketName)
					.object(chunkObject)
					.stream(file.getInputStream(), file.getSize(), -1)
					.contentType(chunkUploadDTO.getFileType())
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
			currentUploadedCount = uploadedChunks.size(); // 立即記錄當前數量
			uploadedChunks.expire(Duration.ofHours(CACHE_EXPIRE_HOURS));

			System.out.println("當前處理Size:" + uploadedChunks.size());

			// 更新已上傳數量
			SysChunkFile updatingFile = baseMapper.selectOne(new LambdaQueryWrapper<SysChunkFile>()
					.eq(SysChunkFile::getFileSha256, chunkUploadDTO.getFileSha256()));

			if (updatingFile != null) {
				updatingFile.setUploadedChunks(currentUploadedCount);
				baseMapper.updateById(updatingFile);
			}

			// 如果所有Chunk 在 Redis 中已獲得（若所有分片已上傳），嘗試合併
			if (currentUploadedCount == chunkUploadDTO.getTotalChunks()) {

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
							Map<String, String> mergeChunksMap = this.mergeChunks(mergedBasePath,
									chunkUploadDTO.getFileSha256(), chunkUploadDTO.getFileName(),
									chunkUploadDTO.getTotalChunks());

							filePath = mergeChunksMap.get("filePath");

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
		return new ChunkResponseVO(currentUploadedCount, chunkUploadDTO.getTotalChunks(),
				chunkUploadDTO.getChunkIndex(), chunkUploadDTO.getFileSha256(), filePath);

	}

	@Override
	public Map<String, String> mergeChunks(String mergedBasePath, String sha256, String fileName, Integer totalChunks) {

		String chunkKey = CHUNK_KEY_SET_PREFIX + sha256;
		String metaKey = META_KEY_PREFIX + sha256;
		// 預先宣告 SysChunkFile 實例
		SysChunkFile sysChunkFile = null;

		RSet<Integer> uploadedSet = redissonClient.getSet(chunkKey);

		try {

			// 如果Redis中上傳的分片數量,未達到最大數量
			if (uploadedSet.size() < totalChunks) {
				System.out.println("Not all chunks have been uploaded");
				return null;
			}

			// 準備合併目標路徑
			String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf(".")) : fileName;
			String fileExt = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")) : "";
			String mergedFilePath = mergedBasePath + baseName + fileExt;

			// 建立 ComposeSources
			System.out.println("建立物件合併列表");
			List<ComposeSource> sources = IntStream.range(0, totalChunks)
					.mapToObj(i -> ComposeSource.builder()
							.bucket(bucketName)
							.object("chunks/" + sha256 + "_" + i)
							.build())
					.collect(Collectors.toList());

			// 在 MinIO 操作前，先進行防禦性 DB 查詢
			sysChunkFile = baseMapper
					.selectOne(new LambdaQueryWrapper<SysChunkFile>().eq(SysChunkFile::getFileSha256, sha256));

			//  檢查 NPE：如果此時記錄就不存在，拋出異常，進入 catch 塊
			if (sysChunkFile == null) {
				log.error(
						"Merge failure: DB record for SHA256={} missing before MinIO operation. Cannot update status.",
						sha256);
				throw new SysChunkFileException("DB record missing: Cannot proceed with merge.");
			}

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

			// 更新這筆資料
			System.out.println("資料庫更新資料");

			sysChunkFile.setFilePath(mergedFilePath);
			sysChunkFile.setUploadedChunks(totalChunks);
			sysChunkFile.setStatus(1);
			sysChunkFile.setFileSize(stat.size());

			baseMapper.updateById(sysChunkFile);

			System.out.println("合併完成");
			return Map.of("filePath", mergedFilePath);
		} catch (Exception e) {

			// 如果 sysChunkFile 已經在 try 塊中被查到，就使用它 (如果沒有被併發刪除)
			if (sysChunkFile != null) {
				// 如果找到了記錄，更新為合併錯誤(Status = 99)
				sysChunkFile.setUploadedChunks(totalChunks);
				sysChunkFile.setStatus(99);
				baseMapper.updateById(sysChunkFile);
			}

			e.printStackTrace();
			log.error("分片合併錯誤", e);

			throw new SysChunkFileException("Large file upload. Part merging failed.");
		} finally {
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

		}
	}

	@Override
	public void downloadFile(String fileId, HttpServletResponse response) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteSysChunkFileByPath(String minioPath) {
		LambdaQueryWrapper<SysChunkFile> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SysChunkFile::getFilePath, minioPath);

		baseMapper.delete(queryWrapper);

	}

}
