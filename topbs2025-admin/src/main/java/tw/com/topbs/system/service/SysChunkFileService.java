package tw.com.topbs.system.service;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
import tw.com.topbs.system.pojo.VO.CheckFileVO;
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
	 * 檔案的分片上傳
	 * 
	 * @param file
	 * @param chunkUploadDTO
	 * @return
	 */
	void uploadChunk(MultipartFile file, @Valid ChunkUploadDTO chunkUploadDTO);

	/**
	 * 分片檔案上傳後的合併
	 * 
	 * @param sha256
	 * @param fileName
	 * @param totalChunks
	 * @return
	 */
	Map<String, String> mergeChunks(String sha256, String fileName, Integer totalChunks);

	/**
	 * 根據fileId,分片下載檔案
	 * 
	 * @param fileId
	 * @param response
	 */
	void downloadFile(String fileId, HttpServletResponse response);

}
