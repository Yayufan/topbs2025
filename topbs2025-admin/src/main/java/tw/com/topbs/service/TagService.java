package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

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
	 * 獲取單一標籤
	 * 
	 * @param tagId
	 * @return
	 */
	Tag getTag(Long tagId);

	/**
	 * 新增標籤
	 * 
	 * @param addTagDTO
	 */
	Long insertTag(AddTagDTO addTagDTO);

	/**
	 * 更新標籤
	 * 
	 * @param putTagDTO
	 */
	void updateTag(PutTagDTO pubTagDTO);

	/**
	 * 根據tagId刪除標籤
	 * 
	 * @param tagId
	 */
	void deleteTag(Long tagId);

	/**
	 * 為複數member 添加/更新/刪除 tag
	 * 
	 * @param memberIdList
	 * @param tagId
	 */
	void assignMemberToTag(List<Long> targetMemberIdList, Long tagId);

	/**
	 * 為複數paper 添加/更新/刪除 tag
	 * 
	 * @param targetPaperIdList
	 * @param tagId
	 */
	void assignPaperToTag(List<Long> targetPaperIdList, Long tagId);

	/**
	 * 為複數paperReviewer 添加/更新/刪除 tag
	 * 
	 * @param targetPaperReviewerIdList
	 * @param tagId
	 */
	void assignPaperReviewerToTag(List<Long> targetPaperReviewerIdList, Long tagId);

	/**
	 * 為複數attendees 添加/更新/刪除 tag
	 * 
	 * @param targetAttendeesIdList
	 * @param tagId
	 */
	void assignAttendeesToTag(List<Long> targetAttendeesIdList, Long tagId);

	/**
	 * 原色 #4A7056（一個深綠色） → 做「同色系明亮度/飽和度漸變」
	 * 
	 * 把顏色轉成 HSL（色相 Hue、飽和度 Saturation、亮度 Lightness）
	 * 
	 * 固定 Hue (色相不變，保持綠色)
	 * 
	 * 小幅調整 S / L (每個 group 差 5-10%)，產生相近色
	 * 
	 * @param hexColor    基本色
	 * @param groupIndex  群組角標(index)
	 * @param stepPercent 每組亮度+5%
	 * @return
	 */
	String adjustColor(String hexColor, int groupIndex, int stepPercent);
	
}
