package tw.com.topbs.service;

import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

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
	 * 為用戶新增/更新/刪除 複數審稿委員
	 * 
	 * @param targetPaperReviewerIdList
	 * @param paperId
	 */
	void assignPaperReviewerToPaper(List<Long> targetPaperReviewerIdList, Long paperId);

	
}
