package tw.com.topbs.strategy.tag;

import java.util.Collection;
import java.util.List;

public interface TagStrategy {
	// 返回 "member" / "attendees" / "paper" / "paper-reviewer"
	String supportType();

	/**
	 * 根據標籤ID，獲取持有該標籤的人數
	 * 
	 * @param tagId
	 * @return
	 */
	long countHoldersByTagId(Long tagId);

	/**
	 * 根據標籤ID，獲取持有這些標籤的 人數
	 * 
	 * @param tagIds
	 * @return
	 */
	long countHoldersByTagIds(Collection<Long> tagIds);

	/**
	 * 根據標籤ID，獲取 關聯的ID 列表,例: <br>
	 * memberTag => 取得memberIdList ;<br>
	 * paperTag => 取得paperIdList
	 * 
	 * @param tagId
	 * @return
	 */
	List<Long> getAssociatedIdsByTagId(Long tagId);
	
	
	/**
	 * 為複數entity 添加/更新/刪除 tag
	 * 
	 * @param entityIdList
	 * @param tagId
	 */
	void assignEntitiesToTag(List<Long> entityIdList, Long tagId);

}
