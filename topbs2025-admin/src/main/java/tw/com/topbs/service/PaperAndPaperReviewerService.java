package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewer;

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
	 * 根據審稿階段,拿到該階段關聯的總數
	 * @param reviewStage
	 * @return
	 */
	long getPaperReviewersByReviewStage(String reviewStage);
	
	/**
	 * 根據審稿階段,拿到該階段審稿人的總數
	 * 
	 * @param reviewStage
	 * @return
	 */
	long getReviewerCountByReviewStage(String reviewStage);
	
	/**
	 * 檢查 reviewer 在某階段是否仍被指派
	 * 
	 * @param reviewStage 審核階段
	 * @param reviewerId 審稿人ID
	 * @return
	 */
	boolean isReviewerStillAssignedInStage(String reviewStage,Long reviewerId);
	
	/**
	 * 獲取 在該移除Tag的審稿人中 , 仍具有應審核稿件的審稿人
	 * 
	 * @param reviewStage
	 * @param paperReviewersToRemove
	 * @return
	 */
	Set<Long> getReviewerIdsStillAssignedInStage(String reviewStage, Collection<Long>paperReviewersToRemove);
	
	
	/**
	 * 批量檢查審稿人在指定階段是否沒有任何任務
	 * @return 返回在該階段沒有任何任務的審稿人ID集合
	 */
	public Set<Long> batchCheckReviewersWithoutAssignment(String reviewStage, Collection<Long> reviewerIds);
	
	
	
	IPage<PaperAndPaperReviewer> getPaperReviewersByReviewerIdAndReviewStage(IPage<PaperAndPaperReviewer> pageable,Long reviewerId,String reviewStage);
	
	/**
	 * 批量刪除 稿件和審稿委員的關聯
	 * 
	 * @param currentPaperAndPaperReviewerMapByReviewerId 審稿者ID和審稿關係的映射
	 * @param paperReviewerIdsToRemove                    要刪除的審稿者ID列表
	 */
	void batchDeletePapersAndReviewers(
			Map<Long, PaperAndPaperReviewer> paperAndReviewersMapByReviewerId,
			Collection<Long> paperReviewerIdsToRemove) ;
	
	/**
	 * 根據稿件ID 和 審稿狀態 , 查詢關連
	 * 
	 * @param paperId
	 * @param reviewStage
	 * @return
	 */
	List<PaperAndPaperReviewer> getPapersAndReviewersByPaperIdAndReviewStage(Long paperId, String reviewStage);

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
	 * 根據審稿階段 去查詢 審稿人對應審稿件的評分狀況
	 * 
	 * @param pageable    稿件 和 審稿人的評分關係
	 * @param reviewStage 審稿階段
	 * @return
	 */
	IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable, String reviewStage);


	/**
	 * 為 處在X階段的 此篇稿件新增審稿人
	 * 
	 * @param paperId 稿件ID
	 * @param reviewStage 審稿狀態
	 * @param reviewerMapById 審稿委員映射對象
	 * @param paperReviewerIdsToAdd 要被新增的審稿委員
	 */
	void addReviewerToPaper(Long paperId, String reviewStage,Map<Long, PaperReviewer> reviewerMapById, Collection<Long> paperReviewerIdsToAdd);
	
	
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
