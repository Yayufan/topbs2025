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
	 * @param insertTagDTO
	 */
	void insertTag(AddTagDTO insertTagDTO);

	/**
	 * 更新標籤
	 * 
	 * @param updateTagDTO
	 */
	void updateTag(PutTagDTO updateTagDTO);

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

}
