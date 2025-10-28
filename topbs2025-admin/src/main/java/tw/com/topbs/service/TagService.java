package tw.com.topbs.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.enums.TagTypeEnum;
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
	 * 校驗 tagIds 並獲得 Tag類型<br>
	 * 當tag的type沒有全部一致時,報錯
	 * 
	 * @param tagIds
	 * @return
	 */
	TagTypeEnum validateAndGetTagType(Collection<Long> tagIds);

	/**
	 * 校驗 tagId 並獲得 Tag類型
	 * 
	 * @param tagId
	 * @return
	 */
	default TagTypeEnum validateAndGetTagType(Long tagId) {
		return this.validateAndGetTagType(Collections.singleton(tagId));
	};

	/**
	 * 根據TagId查詢此tag的持有人數
	 * 
	 * @param tagId
	 * @return
	 */
	long countHoldersByTagId(Long tagId);

	/**
	 * 根據TagIds查詢這些tag的持有人數(tag必須為同一類型)
	 * 
	 * @param tagIds
	 * @return
	 */
	long countHoldersByTagIds(List<Long> tagIds);

	/**
	 * 獲取全部標籤
	 * 
	 * @return
	 */
	List<Tag> getTagList();

	/**
	 * 根據 type 獲取所有標籤
	 * 
	 * @param type
	 * @return
	 */
	List<Tag> getTagListByType(String type);

	/**
	 * 根據type 和 name 獲取標籤
	 * 
	 * @param type
	 * @param name
	 * @return
	 */
	Tag getTagByTypeAndName(String type, String name);

	/**
	 * 根據 類別 和 全模糊匹配姓名 找到Tag
	 * 
	 * @param type      類別
	 * @param fuzzyName 全模糊(%fuzzyName%)，匹配姓名
	 * @return
	 */
	List<Tag> getTagByTypeAndFuzzyName(String type, String fuzzyName);
	
	/**
	 * 根據 類別 和 後模糊匹配姓名 找到Tags
	 * 
	 * @param type  類別
	 * @param tagName 後模糊(tagName%)，匹配姓名
	 * @return
	 */
	List<Tag> getTagsByTypeAndNamePattern(String type, String tagName);
	
	/**
	 * 根據 類別 和 後模糊匹配姓名 找到TagIds
	 * 
	 * @param type
	 * @param tagName
	 * @return
	 */
	Set<Long> getTagIdsByTypeAndNamePattern(String type, String tagName);

	/**
	 * 查詢處在這個tagIds 的所有Tag
	 * 
	 * @param tagIds
	 * @return
	 */
	List<Tag> getTagListByIds(Collection<Long> tagIds);

	/**
	 * 獲取全部標籤(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<Tag> getTagPage(Page<Tag> page);

	/**
	 * 根據類型，獲取全部標籤(分頁)
	 * 
	 * @param page
	 * @param type
	 * @return
	 */
	IPage<Tag> getTagPageByType(Page<Tag> page, String type);

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
	 * 根據標籤ID 返回 關聯的ID List,例:<br>
	 * member => memberTag ID List<br>
	 * attendees => attendeesTag ID List
	 * 
	 * @param tagId
	 * @return
	 */
	List<Long> getAssociatedIdsByTagId(Long tagId);

	/**
	 * 為複數 實體類 添加/更新/刪除 tag,例:<br>
	 * member => memberTag <br>
	 * attendees => attendeesTag
	 * 
	 * @param targetAssociatedIdList
	 * @param tagId
	 */
	void assignAssociatedToTag(List<Long> targetAssociatedIdList, Long tagId);

	/**
	 * 獲取或創建分組Tag
	 * 
	 * @param tagType             群組名稱,依Table名稱傳入，代表這是哪個數據表需要的tag
	 * @param namePrefix          標籤名前墜, XXX-group-01，就是那個XXX為namePrefix
	 * @param groupIndex          腳標 1 以上
	 * @param baseColor           基本顏色 Hex 格式
	 * @param descriptionTemplate 標籤描述
	 * @return
	 */
	Tag getOrCreateGroupTag(String tagType, String namePrefix, int groupIndex, String baseColor,
			String descriptionTemplate);

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

	/**
	 * 創建或獲取 Accepted(一階段通過) 狀態Tag
	 * 
	 * @param groupIndex
	 * @return
	 */
	public Tag getOrCreateAcceptedGroupTag(int groupIndex);

	/**
	 * 創建或獲取 Accepted_stage_2(二階段通過) 狀態Tag
	 * 
	 * @param groupIndex
	 * @return
	 */
	public Tag getOrCreateAcceptedStage2GroupTag(int groupIndex);

	/**
	 * 創建或獲取 Rejected(一階段駁回) 狀態Tag
	 * 
	 * @param groupIndex
	 * @return
	 */
	public Tag getOrCreateRejectedGroupTag(int groupIndex);

	/**
	 * 創建或獲取 Rejected_stage_2(二階段駁回) 狀態Tag
	 * 
	 * @param groupIndex
	 * @return
	 */
	public Tag getOrCreateRejectedStage2GroupTag(int groupIndex);

	/**
	 * 獲取或創建FirstReviewerGroupTag
	 * 
	 * @param groupIndex
	 * @return
	 */
	Tag getOrCreateFirstReviewerGroupTag(int groupIndex);

	/**
	 * 獲取或創建SecondReviewerGroupTag
	 * 
	 * @param groupIndex
	 * @return
	 */
	Tag getOrCreateSecondReviewerGroupTag(int groupIndex);

}
