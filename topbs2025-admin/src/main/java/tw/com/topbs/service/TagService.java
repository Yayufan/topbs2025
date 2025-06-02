package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutTagDTO;
import tw.com.topbs.pojo.entity.Tag;

/**
 * <p>
 * 標籤表,用於對Member進行分組 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
public interface TagService extends IService<Tag> {

	/**
	 * 獲取全部標籤
	 * 
	 * @return
	 */
	List<Tag> getAllTag();

	/**
	 * 根據 type 獲取所有標籤
	 * 
	 * @param type
	 * @return
	 */
	List<Tag> getAllTagByType(String type);
	
	/**
	 * 根據type 和 name 獲取標籤
	 * 
	 * @param type
	 * @param name
	 * @return
	 */
	Tag getTagByTypeAndName(String type, String name);

	/**
	 * 查詢處在這個tagIds 的所有Tag
	 * 
	 * @param tagIds
	 * @return
	 */
	List<Tag> getTagByTagIds(Collection<Long> tagIds);

	/**
	 * 獲取全部標籤(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<Tag> getAllTag(Page<Tag> page);

	/**
	 * 根據類型，獲取全部標籤(分頁)
	 * 
	 * @param page
	 * @param type
	 * @return
	 */
	IPage<Tag> getAllTag(Page<Tag> page, String type);
	
	/**
	 * 根據 與會者 和 標籤 關聯關係 的映射，拿到 與會者 和 真正標籤的映射
	 * 
	 * @param attendeesTagMap
	 * @return
	 */
	Map<Long, Tag> getTagMapFromAttendeesTag(Map<Long, List<Long>> attendeesTagMap);
	
	
	/**
	 * 獲取單一標籤
	 * 
	 * @param tagId
	 * @return
	 */
	Tag getTag(Long tagId);

	/**
	 * 新增標籤，返回tagId
	 * 
	 * @param addTagDTO
	 * @return
	 */
	Long insertTag(AddTagDTO addTagDTO);

	/**
	 * 更新標籤
	 * 
	 * @param putTagDTO
	 */
	void updateTag(PutTagDTO putTagDTO);

	/**
	 * 根據tagId刪除標籤
	 * 
	 * @param tagId
	 */
	void deleteTag(Long tagId);

	/**
	 * 根據標籤ID 返回memberIdList
	 * 
	 * @param tagId
	 * @return
	 */
	List<Long> getMemberIdListByTagId(Long tagId); 
	
	/**
	 * 為複數member 添加/更新/刪除 tag
	 * 
	 * @param memberIdList
	 * @param tagId
	 */
	void assignMemberToTag(List<Long> targetMemberIdList, Long tagId);
	

	/**
	 * 根據標籤ID 返回paperIdList
	 * 
	 * @param tagId
	 * @return
	 */
	List<Long> getPaperIdListByTagId(Long tagId); 

	/**
	 * 為複數paper 添加/更新/刪除 tag
	 * 
	 * @param targetPaperIdList
	 * @param tagId
	 */
	void assignPaperToTag(List<Long> targetPaperIdList, Long tagId);

	
	/**
	 * 根據標籤ID 返回paperReviewerIdList
	 * 
	 * @param tagId
	 * @return
	 */
	List<Long> getPaperReviewerIdListByTagId(Long tagId); 
	
	
	/**
	 * 為複數paperReviewer 添加/更新/刪除 tag
	 * 
	 * @param targetPaperReviewerIdList
	 * @param tagId
	 */
	void assignPaperReviewerToTag(List<Long> targetPaperReviewerIdList, Long tagId);

	/**
	 * 根據標籤ID 返回attendeesIdList
	 * 
	 * @param tagId
	 * @return
	 */
	List<Long> getAttendeesIdListByTagId(Long tagId); 
	
	/**
	 * 為複數attendees 添加/更新/刪除 tag
	 * 
	 * @param targetAttendeesIdList
	 * @param tagId
	 */
	void assignAttendeesToTag(List<Long> targetAttendeesIdList, Long tagId);
	
	/**
	 * 獲取或創建MemberGroupTag
	 * 
	 * @param groupIndex 分組的索引,需 >= 1
	 * @return
	 */
	Tag getOrCreateMemberGroupTag(int groupIndex);

	/**
	 * 獲取或創建AttendeesGroupTag
	 * 
	 * @param groupIndex 分組的索引,需 >= 1
	 * @return
	 */
	Tag getOrCreateAttendeesGroupTag(int groupIndex);
	
	
	/**
	 * 獲取或創建PaperGroupTag 
	 * 
	 * @param groupIndex
	 * @return
	 */
	Tag getOrCreatePaperGroupTag(int groupIndex);
}
