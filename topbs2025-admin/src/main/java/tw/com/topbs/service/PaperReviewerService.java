package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.PaperReviewer;

public interface PaperReviewerService extends IService<PaperReviewer> {

	/**
	 * 根據 paperReviewerId 獲取審稿委員
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	PaperReviewerVO getPaperReviewer(Long paperReviewerId);

	/**
	 * 查詢符合能審核稿件類別的評審
	 * 
	 * @param absType
	 * @return
	 */
	List<PaperReviewer> getPaperReviewerListByAbsType(String absType);

	/**
	 * 查詢所有審稿委員
	 * 
	 * @return
	 */
	List<PaperReviewerVO> getPaperReviewerList();

	/**
	 * 查詢所有審稿委員(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<PaperReviewerVO> getPaperReviewerPage(Page<PaperReviewer> page);

	/**
	 * 新增審稿委員
	 * 
	 * @param addPaperReviewerDTO
	 */
	void addPaperReviewer(AddPaperReviewerDTO addPaperReviewerDTO);

	/**
	 * 修改審稿委員
	 * 
	 * @param putPaperReviewerDTO
	 */
	void updatePaperReviewer(PutPaperReviewerDTO putPaperReviewerDTO);

	/**
	 * 刪除審稿委員
	 * 
	 * @param paperReviewerId
	 */
	void deletePaperReviewer(Long paperReviewerId);

	/**
	 * 批量刪除審稿委員
	 * 
	 * @param paperReviewerIds
	 */
	void deletePaperReviewerList(List<Long> paperReviewerIds);

}
