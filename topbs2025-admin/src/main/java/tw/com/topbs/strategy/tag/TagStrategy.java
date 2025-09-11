package tw.com.topbs.strategy.tag;

import java.util.Collection;

public interface TagStrategy {
	// 返回 "member" / "attendees" / "paper" / "paper-reviewer"
	String supportType(); 

	/**
	 * 根據標籤ID，獲取持有該標籤的人數
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
	
}
