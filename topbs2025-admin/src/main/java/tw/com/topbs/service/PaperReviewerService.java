package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.dev33.satoken.stp.SaTokenInfo;
import tw.com.topbs.pojo.DTO.PaperReviewerLoginInfo;
import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
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
	List<PaperReviewer> getReviewerListByAbsType(String absType);
	


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
	 * 根據審稿階段 去查詢 審稿人對應審稿件的評分狀況
	 * 
	 * @param pageable    稿件 和 審稿人的評分關係
	 * @param reviewStage 審稿階段
	 * @return
	 */
	IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable, String reviewStage);

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

	/**
	 * 為 審稿委員 新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param paperReviewerId
	 */
	void assignTagToPaperReviewer(List<Long> targetTagIdList, Long paperReviewerId);

	/**
	 * 立刻寄信
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的PaperReviewers
	 * 如果沒有傳任何tag則是寄給所有PaperReviewers
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void sendEmailToPaperReviewers(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 排程寄信
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的PaperReviewers
	 * 如果沒有傳任何tag則是寄給所有PaperReviewers
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void scheduleEmailToReviewers(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 為審稿者替換 merge tag,成為個人化信件
	 * 
	 * @param content       信件內容
	 * @param paperReviewer 替換資料來源
	 * @return
	 */
	String replacePaperReviewerMergeTag(String content, PaperReviewer paperReviewer);

	/** 以下為審稿委員自行使用的API */

	/**
	 * 審稿委員登入
	 * 
	 * @param paperReviewerLoginInfo
	 * @return
	 */
	SaTokenInfo login(PaperReviewerLoginInfo paperReviewerLoginInfo);

	/**
	 * 審稿委員登出
	 * 
	 */
	void logout();

	/**
	 * 透過token從緩存中取得資料
	 * 
	 * @return
	 */
	PaperReviewer getPaperReviewerInfo();

	/**
	 * 根據審稿委員ID、審核階段，獲得要審稿的稿件對象 (分頁)
	 * 
	 * @param pageable
	 * @param reviewerId
	 * @param reivewStage
	 * @return
	 */
	IPage<ReviewVO> getReviewVOPageByReviewerIdAndReviewStage(IPage<PaperAndPaperReviewer> pageable, Long reviewerId,
			String reivewStage);

	/**
	 * 審稿委員對稿件進行審核
	 * 
	 * @param putPaperReviewDTO
	 */
	void submitReviewScore(PutPaperReviewDTO putPaperReviewDTO);

}
