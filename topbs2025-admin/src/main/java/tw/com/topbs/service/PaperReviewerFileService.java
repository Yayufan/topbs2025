package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.validation.Valid;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerFileDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerFile;

/**
 * <p>
 * 給審稿委員的公文檔案和額外]資料 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-06-03
 */

@Validated
public interface PaperReviewerFileService extends IService<PaperReviewerFile> {

	/**
	 * 根據審稿委員ID 獲取 審稿委員公文附件列表
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	List<PaperReviewerFile> getReviewerFilesByReviewerId(Long reviewerId);
	
	/**
	 * 根據 paperReviewerIds 找到對應複數審稿委員的，公文檔案附件
	 * 
	 * @param paperReviewerIds
	 * @return
	 */
	List<PaperReviewerFile> getReviewerFilesByReviewerIds(Collection<Long> paperReviewerIds);

	/**
	 * 根據 reviewerIds 獲取審稿委員中具有的公文檔案 , 以paperReviewerId為鍵,PaperReviewerFileList為值的方式返回
	 * 
	 * @param reviewerIds
	 * @returnkey 為 paperReviewerId , value 為PaperReviewerFileList
	 */
	Map<Long, List<PaperReviewerFile>> getReviewerFileMapByReviewerId(Collection<Long> reviewerIds);
	

	/**
	 * 根據 reviewerList 範圍內審稿委員中具有的公文檔案 , 以paperReviewerId為鍵,PaperReviewerFileList為值的方式返回
	 * 
	 * @param reviewerList
	 * @return 為 paperReviewerId , value 為PaperReviewerFileList
	 */
	Map<Long, List<PaperReviewerFile>> getReviewerFileMapByReviewerId(List<PaperReviewer> reviewerList);
	
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
	void updatePaperReviewerFile(MultipartFile file, @Valid PutPaperReviewerFileDTO putPaperReviewerFileDTO);

	/**
	 * 根據 主鍵ID 刪除附件檔案
	 * 
	 * @param reviewerFileId
	 */
	void deleteReviewerFileById(Long reviewerFileId);

}
