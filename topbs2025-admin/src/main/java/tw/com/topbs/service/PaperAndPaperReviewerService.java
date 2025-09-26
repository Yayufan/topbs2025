package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;

/**
 * <p>
 * 投稿-審稿委員 關聯表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PaperAndPaperReviewerService extends IService<PaperAndPaperReviewer> {

	/**
	 * 根據稿件ID,獲取已經分配的評審列表
	 * 
	 * @param paperId
	 * @return
	 */
	List<AssignedReviewersVO> getAssignedReviewersByPaperId(Long paperId);
	
	/**
	 * 根據審核階段,獲得 根據paperId分組，獲得映射對象
	 * 
	 * @param reviewStage
	 * @return
	 */
	Map<Long, List<PaperAndPaperReviewer>> groupPaperReviewersByPaperId(String reviewStage);

	/**
	 * 根據paperId分組，獲得映射對象
	 * 
	 * @param paperIds
	 * @return key為paperId,value為 已分發帶狀態的審稿委員列表 的Map
	 */
	Map<Long, List<AssignedReviewersVO>> getAssignedReviewersMapByPaperId(Collection<Long> paperIds);
	
	/**
	 * 根據paperId分組，獲得映射對象
	 * 
	 * @param paperList
	 * @return
	 */
	Map<Long, List<AssignedReviewersVO>> getAssignedReviewersMapByPaperId(List<Paper> paperList);

	/**
	 * 根據審稿委員ID，獲得要審稿的稿件關聯
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	List<PaperAndPaperReviewer> getPapersAndReviewersByReviewerId(Long paperReviewerId);

	/**
	 * 根據審稿委員ID，獲得要審稿的 第一階段稿件對象 (分頁)
	 * 
	 * @param pageable
	 * @param reviewerId
	 * @return
	 */

	IPage<ReviewVO> getReviewVOPageByReviewerIdAtFirstReview(IPage<PaperAndPaperReviewer> pageable, Long reviewerId);

	/**
	 * 根據審稿委員ID，獲得要審稿的 第二階段稿件對象 (分頁)
	 * 
	 * @param pageable
	 * @param reviewerId
	 * @return
	 */
	IPage<ReviewVO> getReviewVOPageByReviewerIdAtSecondReview(IPage<PaperAndPaperReviewer> pageable, Long reviewerId);

	/**
	 * 根據審稿階段 去查詢 審稿人對應審稿件的評分狀況
	 * 
	 * @param pageable    稿件 和 審稿人的評分關係
	 * @param reviewStage 審稿階段
	 * @return
	 */
	IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable, String reviewStage);

	/**
	 * 只要審稿委員符合稿件類型，且沒有相同審核階段的記錄，就自動進行分配
	 * 
	 */
	void autoAssignPaperReviewer(String reviewStage);

	/**
	 * 為用戶新增/更新/刪除 複數審稿委員
	 * 
	 * @param reviewStage               審核階段
	 * @param targetPaperReviewerIdList
	 * @param paperId
	 */
	void assignPaperReviewerToPaper(String reviewStage, List<Long> targetPaperReviewerIdList, Long paperId);

	/**
	 * 提交或更新審稿委員的評分和狀態。
	 * 這個方法將根據提供的 DTO 更新現有的評審記錄。
	 *
	 * @param updateDto 包含評審記錄 ID、評分、狀態和更新者的 DTO
	 * @return boolean 表示操作是否成功
	 */

	/**
	 * 提交或更新審稿委員的評分和狀態
	 * 這個方法將根據提供的 DTO 更新現有的評審記錄。
	 * 
	 * @param putPaperReviewDTO 包含各種ID、評分、審核階段狀態
	 */
	void submitReviewScore(PutPaperReviewDTO putPaperReviewDTO);

	/**
	 * 根據審稿階段 和 審稿人ID 判斷是否評分結束
	 * 
	 * @param reviewStage     審稿階段
	 * @param paperReviewerId 審稿人ID
	 * @return
	 */
	Boolean isReviewFinished(String reviewStage, Long paperReviewerId);

}
