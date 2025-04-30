package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.pojo.entity.Tag;

/**
 * <p>
 * paper表 和 tag表的關聯表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
public interface PaperTagService extends IService<PaperTag> {

	/**
	 * 根據 paperId 查詢與之有關的所有Tag
	 * 
	 * @param paperId
	 * @return
	 */
	List<Tag> getTagByPaperId(Long paperId);

	/**
	 * 根據 tagId 查詢與之有關的所有Paper
	 * 
	 * @param tagId
	 * @return
	 */
	List<Paper> getPaperByTagId(Long tagId);

	/**
	 * 為 稿件 新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param paperId
	 */
	void assignTagToPaper(List<Long> targetTagIdList, Long paperId);

}
