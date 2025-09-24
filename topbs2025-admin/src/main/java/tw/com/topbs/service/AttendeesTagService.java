package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.pojo.entity.Tag;

/**
 * <p>
 * 與會者 與 標籤 的關聯表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
public interface AttendeesTagService extends IService<AttendeesTag> {

	
	/**
	 * 根據 attendeesId 查詢與之有關tagIds關聯
	 * 
	 * @param attendeesId
	 * @return
	 */
	Set<Long> getTagIdsByAttendeesId(Long attendeesId);
	
	/**
	 * 根據 attendeesId 查詢與之有關的所有Tag關聯
	 * 
	 * @param attendeesId
	 * @return
	 */
	List<AttendeesTag> getAttendeesTagByAttendeesId(Long attendeesId);
	
	
	
	/**
	 * 拿到與會者持有的Tag
	 * @param attendeesId
	 * @return
	 */
	List<Tag> getTagsByAttendeesId(Long attendeesId);
	
	/**
	 * 根據 attendeesIds 查詢 與會者ID 和 標籤 關聯關係 的映射
	 * 
	 * @param attendeesIds
	 * @return
	 */
	Map<Long, List<Long>> getAttendeesTagMapByAttendeesIds(Collection<Long> attendeesIds);

	/**
	 * 根據 attendeesList, 獲取範圍內與會者的 標籤映射對象
	 * 
	 * @param attendeesList
	 * @return 獲得以attendeesId為key , List<Tag> 為值的 映射對象
	 */
	Map<Long, List<Tag>> getTagMapByAttendeesId(Collection<Attendees> attendeesList);
	
	/**
	 * 根據 tagId 查詢與之有關的所有Attendees關聯
	 * 
	 * @param tagId
	 * @return
	 */
	List<AttendeesTag> getAttendeesTagByTagId(Long tagId);

	/**
	 * 根據複數 attendeesId 查詢與之有關的所有Tag關聯
	 * 
	 * @param attendeesIds
	 * @return
	 */
	List<AttendeesTag> getAttendeesTagByAttendeesIds(Collection<Long> attendeesIds);

	/**
	 * 根據複數 tagId 查詢與之有關的所有Tag關聯
	 * 
	 * @param tagIds
	 * @return
	 */
	List<AttendeesTag> getAttendeesTagByTagIds(Collection<Long> tagIds);

	/**
	 * 為一個tag和attendees新增關聯
	 * 
	 * @param attendeesTag
	 */
	void addAttendeesTag(AttendeesTag attendeesTag);
	
	/**
	 * 透過與會者ID 和 標籤ID 建立關聯
	 * 
	 * @param attendeesId 與會者ID
	 * @param tagId 標籤ID
	 */
	 void addAttendeesTag(Long attendeesId, Long tagId);

	 /**
	  * 為與會者新增多個Tag
	  * 
	  * @param attendeesId
	  * @param tagsToAdd
	  */
	 void addTagsToAttendees(Long attendeesId, Collection<Long> tagsToAdd);

	/**
	 * 根據參加者 ID 刪除多個標籤關聯
	 * 
	 * @param attendeesId
	 * @param tagsToRemove
	 */
	void removeTagsFromAttendee(Long attendeesId, Collection<Long> tagsToRemove);
	
	/**
	 * 根據標籤 ID 刪除多個與會者 關聯
	 * 
	 * @param tagId
	 * @param attendeessToRemove
	 */
	void removeAttendeesFromTag(Long tagId, Set<Long> attendeessToRemove);
	

	
}
