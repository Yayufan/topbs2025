package tw.com.topbs.service;

import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.PaperReviewerTag;

/**
 * <p>
 * paperReviewer表 和 tag表的關聯表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
public interface PaperReviewerTagService extends IService<PaperReviewerTag> {

	/**
	 * 根據 tagId 查詢與之有關的所有PaperReviewer關聯
	 * 
	 * @param tagId
	 * @return
	 */
	List<PaperReviewerTag> getPaperReviewerTagByTagId(Long tagId);

	/**
	 * 為一個tag和paperReviewer新增關聯
	 * 
	 * @param paperReviewerTag
	 */
	void addPaperReviewerTag(PaperReviewerTag paperReviewerTag);

	/**
	 * 移除此 tag 與多位 paperReviewer 關聯
	 * 
	 * @param tagId
	 * @param paperReviewersToRemove
	 */
	void removeTagRelationsForPaperReviewers(Long tagId, Set<Long> paperReviewersToRemove);
}
