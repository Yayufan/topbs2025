package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
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
	 * 只要審稿委員符合稿件類型，就自動進行分配
	 * 
	 */
	void autoAssignPaperReviewer();

	/**
	 * 為用戶新增/更新/刪除 複數審稿委員
	 * 
	 * @param targetPaperReviewerIdList
	 * @param paperId
	 */
	void assignPaperReviewerToPaper(List<Long> targetPaperReviewerIdList, Long paperId);

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

}
