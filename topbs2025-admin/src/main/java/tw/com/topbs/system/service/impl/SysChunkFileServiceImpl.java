package tw.com.topbs.system.service.impl;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.system.mapper.SysChunkFileMapper;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
import tw.com.topbs.system.pojo.VO.CheckFileVO;
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
	public CheckFileVO checkFile(String md5) {

		// 透過MD5值，查找資料庫有沒有這筆資料
		LambdaQueryWrapper<SysChunkFile> sysChunkFileWrapper = new LambdaQueryWrapper<>();
		sysChunkFileWrapper.eq(SysChunkFile::getFileMd5, md5);
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
	public void uploadChunk(MultipartFile file, ChunkUploadDTO chunkUploadDTO) {
		// TODO Auto-generated method stub
		String chunkKey = CHUNK_KEY_SET_PREFIX + chunkUploadDTO.getFileMd5();
		String metaKey = META_KEY_PREFIX + chunkUploadDTO.getFileMd5();

		try {
			RSet<Integer> uploadedChunks = redissonClient.getSet(chunkKey);

			// 支援斷點續傳，當這個chunk Redis已經有這個檔案分片時，直接返回，不用重新上傳一次
			if (uploadedChunks.contains(chunkUploadDTO.getChunkIndex())) {
				return;
			}

			// 創建臨時檔案(分片)，最後會直接在minio中進行合併
			String chunkObject = "chunks/" + chunkUploadDTO.getFileMd5() + "_" + chunkUploadDTO.getChunkIndex();

			// 上傳分片至 MinIO
			minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(chunkObject)
					.stream(file.getInputStream(), file.getSize(), -1).contentType(file.getContentType()).build());

			// 獲取redis中關於這個分片檔案的Map資訊
			RMap<String, Object> metaMap = redissonClient.getMap(metaKey);

			// 如果上傳的是第一個分塊，且metaMap是空，則初始化 Redis Meta 資訊，
			if (chunkUploadDTO.getChunkIndex() == 0 && metaMap.isEmpty()) {
				metaMap.put("totalChunks", chunkUploadDTO.getTotalChunks());
				metaMap.put("fileName", file.getOriginalFilename());
				metaMap.expire(Duration.ofHours(72));

				// 建立 DB 紀錄
				SysChunkFile sysChunkFile = new SysChunkFile();
				sysChunkFile.setFileId(UUID.randomUUID().toString());
				sysChunkFile.setFileMd5(chunkUploadDTO.getFileMd5());
				sysChunkFile.setFileName(file.getOriginalFilename());
				// 尚未完成全部上傳
				sysChunkFile.setStatus(0);
				sysChunkFile.setTotalChunks(chunkUploadDTO.getTotalChunks());
				// 因為剛剛已經成功上傳，這邊DB中以上傳的Chunk改為1
				sysChunkFile.setUploadedChunks(1);
				baseMapper.insert(sysChunkFile);
			}

			// Redis中記錄此分片已上傳
			uploadedChunks.add(chunkUploadDTO.getChunkIndex());
			uploadedChunks.expire(Duration.ofHours(72));

			// 更新已上傳數量
			SysChunkFile updatingFile = baseMapper.selectOne(
					new LambdaQueryWrapper<SysChunkFile>().eq(SysChunkFile::getFileMd5, chunkUploadDTO.getFileMd5()));

			if (updatingFile != null) {
				updatingFile.setUploadedChunks(uploadedChunks.size());
				baseMapper.updateById(updatingFile);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public Map<String, String> mergeChunks(String md5, String fileName, Integer totalChunks) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void downloadFile(String fileId, HttpServletResponse response) {
		// TODO Auto-generated method stub

	}

}
