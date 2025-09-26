package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;

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
	 * 根據 paperReviewerId 查詢與之有關的所有Tag
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	List<Tag> getTagByPaperReviewerId(Long paperReviewerId);
	

	/**
	 * 根據 paperReviewerIds 獲取審稿委員中具有的tag , 以paperReviewerId為鍵,tagList為值的方式返回
	 * 
	 * @param paperReviewerIds 
	 * @return key 為 paperReviewerId , value 為tagList
	 */
	Map<Long, List<Tag>> groupTagsByPaperReviewerId(Collection<Long> paperReviewerIds );
	
	
	/**
	 * 根據 tagId 查詢與之有關的所有PaperReviewer關聯
	 * 
	 * @param tagId
	 * @return
	 */
	List<PaperReviewerTag> getPaperReviewerTagByTagId(Long tagId);
	
	/**
	 * 根據paperReviewerIds 和 tagIds 獲得關聯
	 * 
	 * @param paperReviewerIds
	 * @param tagIds
	 * @return
	 */
	List<PaperReviewerTag> getPaperReviewerTagByReviewerIdsAndTagIds(Collection<Long> paperReviewerIds,Collection<Long> tagIds);
	

	/**
	 * 為一個tag和paperReviewer新增關聯
	 * 
	 * @param paperReviewerTag
	 */
	void addPaperReviewerTag(PaperReviewerTag paperReviewerTag);
	
	/**
	 * 為一個tag和paperReviewer新增關聯
	 * 
	 * @param paperReviewerId
	 * @param tagId
	 */
	void addPaperReviewerTag(Long paperReviewerId, Long tagId);
	
	/**
	 * 批量為審稿委員新增tagId
	 * 
	 * @param reviewerIds
	 * @param tagId
	 */
	public void addPaperReviewerTagsBatch(Collection<Long> reviewerIds, Long tagId);

	
	/**
	 * 為一個tag和 paperReviewer 移除關聯
	 * 
	 * @param paperReviewerId
	 * @param tagId
	 */
	void removePaperReviewerTag(Long paperReviewerId, Long tagId);
	
	/**
	 * 根據paperReviewerId 和 tagIds 移除關聯
	 * 
	 * @param paperReviewerId
	 * @param tagIds
	 */
	void removePaperReviewerTag(Long paperReviewerId, Collection<Long> tagIds);
	
	/**
	 * 將 複數tag 與 審稿委員 建立關係
	 * 
	 * @param targetTagIdList
	 * @param paperReviewerId
	 */
	void assignTagToPaperReviewer(Long paperReviewerId,List<Long> targetTagIdList );
	
	
	/**
	 * 根據標籤 ID 刪除多個審稿委員 關聯
	 * 
	 * @param tagId
	 * @param paperReviewersToRemove
	 */
	void removePaperReviewerFromTag(Long tagId, Set<Long> paperReviewersToRemove);
}
