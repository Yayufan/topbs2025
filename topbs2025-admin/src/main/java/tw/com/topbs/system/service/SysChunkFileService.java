package tw.com.topbs.system.service;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
import tw.com.topbs.system.pojo.VO.CheckFileVO;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.pojo.entity.SysChunkFile;

/**
 * <p>
 * 系統通用，大檔案分片上傳，5MB以上就可處理，這邊僅記錄這個大檔案的上傳進度 和 狀況，
 * 合併後的檔案在minio，真實的分片區塊，會放在臨時資料夾，儲存資料會在redis 服務類
 * </p>
 *
 * @author Joey
 * @since 2025-04-07
 */

@Validated
public interface SysChunkFileService extends IService<SysChunkFile> {

	/**
	 * 前端傳送檔案的SHA256值，用來判斷是否已經存在過這個檔案，用於實現秒傳
	 * 
	 * @param sha256
	 * @return
	 */
	CheckFileVO checkFile(String sha256);

	/**
	 * 檔案的分片上傳(指定儲存路徑)
	 * 
	 * @param file           檔案分片
	 * @param mergedBasePath 合併的放置路徑，要以/結尾
	 * @param chunkUploadDTO 檔案資訊
	 * @return
	 */
	ChunkResponseVO uploadChunk(MultipartFile file, String mergedBasePath, @Valid ChunkUploadDTO chunkUploadDTO);

	/**
	 * 分片檔案上傳後的合併(指定儲存路徑)
	 * 
	 * @param mergedBasePath 合併的放置路徑，要以/結尾
	 * @param sha256
	 * @param fileName
	 * @param totalChunks
	 * @return
	 */
	Map<String, String> mergeChunks(String mergedBasePath, String sha256, String fileName, Integer totalChunks);

	/**
	 * 根據fileId,分片下載檔案
	 * 
	 * @param fileId
	 * @param response
	 */
	void downloadFile(String fileId, HttpServletResponse response);

	/**
	 * 根據minio 中的path , 找到DB 的紀錄並刪除
	 * 
	 * @param minioPath
	 */
	void deleteSysChunkFileByPath(String minioPath);

}
