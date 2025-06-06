package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerFileDTO;
import tw.com.topbs.pojo.entity.PaperReviewerFile;

/**
 * <p>
 * 給審稿委員的公文檔案和額外]資料 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-06-03
 */
public interface PaperReviewerFileService extends IService<PaperReviewerFile> {

	/**
	 * 根據審稿委員ID 獲取 審稿委員公文附件列表
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	List<PaperReviewerFile> getPaperReviewerFilesByPaperReviewerId(Long paperReviewerId);
	
	/**
	 * 根據 paperReviewerIds 找到對應複數審稿委員的，公文檔案附件
	 * 
	 * @param paperReviewerIds
	 * @return
	 */
	List<PaperReviewerFile> getPaperReviewerFilesPaperReviewerIds(Collection<Long> paperReviewerIds);

	/**
	 * 根據 paperReviewerIds 獲取審稿委員中具有的公文檔案 , 以paperReviewerId為鍵,PaperReviewerFileList為值的方式返回
	 * 
	 * @param paperReviewerIds
	 * @returnkey 為 paperReviewerId , value 為PaperReviewerFileList
	 */
	Map<Long, List<PaperReviewerFile>> groupFilesByPaperReviewerId(Collection<Long> paperReviewerIds );
	

	
	/**
	 * 為審稿委員新增附件檔案
	 * 
	 * @param file
	 * @param paperReviewerId
	 */
	void addPaperReviewerFile(MultipartFile file, Long paperReviewerId);

	/**
	 * 為審稿委員更新附件檔案
	 * 
	 * @param file
	 * @param putPaperReviewerFileDTO
	 */
	void updatePaperReviewerFile(MultipartFile file, PutPaperReviewerFileDTO putPaperReviewerFileDTO);

	/**
	 * 根據 paperFileUploadId 刪除附件檔案
	 * 
	 * @param paperFileUploadId
	 */
	void deletePaperReviewerFile(Long paperFileUploadId);

}
