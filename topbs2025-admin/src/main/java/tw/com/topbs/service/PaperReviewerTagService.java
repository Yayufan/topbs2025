package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.PaperReviewer;
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
	 * 根據審稿者ID , 獲取關聯
	 * 
	 * @param reviewerId
	 * @return
	 */
	List<PaperReviewerTag> getReviewerTagsByReviewerId(Long reviewerId);

	/**
	 * 根據reviewerId拿到 tagIds
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	Set<Long> getTagIdsByReviewerId(Long paperReviewerId);

	/**
	 * 根據 paperReviewerId 查詢與之有關的所有Tag
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	List<Tag> getTagsByPaperReviewerId(Long paperReviewerId);

	/**
	 * 查詢符合審稿者ID 範圍的關聯
	 * 
	 * @param reviewerIds
	 * @return
	 */
	List<PaperReviewerTag> getReviewerTagsByReviewerIds(Collection<Long> reviewerIds);

	/**
	 * 根據 paperReviewerIds 獲取審稿委員中具有的tag , 以paperReviewerId為鍵,tagList為值的方式返回
	 * 
	 * @param reviewerIds
	 * @return key 為 reviewerId , value 為tagList
	 */
	Map<Long, List<Tag>> getReviewerTagMapByReviewerId(Collection<Long> reviewerIds);

	/**
	 * 根據 paperReviewerIds 獲取審稿委員中具有的tag , 以paperReviewerId為鍵,tagList為值的方式返回
	 * 
	 * @param reviewerIds
	 * @return key 為 reviewerId , value 為tagList
	 */

	/**
	 * 根據 reviewerList 範圍內審稿委員中具有的tag , 以paperReviewerId為鍵,tagList為值的方式返回
	 * 
	 * @param reviewerList
	 * @return key 為 reviewerId , value 為tagList
	 */
	Map<Long, List<Tag>> getReviewerTagMapByReviewerId(List<PaperReviewer> reviewerList);

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
	List<PaperReviewerTag> getReviewerTagByReviewerIdsAndTagIds(Collection<Long> paperReviewerIds,
			Collection<Long> tagIds);

	/**
	 * 為一個tag和paperReviewer新增關聯
	 * 
	 * @param paperReviewerTag
	 */
	void addPaperReviewerTag(PaperReviewerTag paperReviewerTag);


	/**
	 * 為一個tag和 paperReviewer 移除關聯
	 * 
	 * @param paperReviewerId
	 * @param tagId
	 */
	void removePaperReviewerTag(Long paperReviewerId, Long tagId);


	/**
	 * 為審稿委員 與 複數標籤 建立關聯
	 * 
	 * @param targetTagIdList
	 * @param paperReviewerId
	 */
	void addTagsToReviewer(Long paperReviewerId, Collection<Long> tagsToAdd);

	/**
	 * 為標籤 與 複數審稿委員 建立關聯
	 * 
	 * @param tagId
	 * @param reviewerToAdd
	 */
	void addReviewersToTag(Long tagId, Collection<Long> reviewersToAdd);
	
	/**
	 * 為標籤 與 複數審稿委員 建立不重複的關聯
	 * 
	 * @param tagId
	 * @param reviewersToAdd
	 */
	void addUniqueReviewersToTag(Long tagId, Collection<Long> reviewersToAdd);

	/**
	 * 為審稿委員 刪除多個 tag關聯
	 * 
	 * @param paperReviewerId
	 * @param tagIds
	 */
	void removeTagsFromReviewer(Long paperReviewerId, Collection<Long> tagsToRemove);
	
	/**
	 * 為標籤 刪除多個 審稿委員關聯
	 * 
	 * @param tagId
	 * @param paperReviewersToRemove
	 */
	void removeReviewersFromTag(Long tagId, Set<Long> reviewersToRemove);
	



}
