package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	 * 根據 paperIds 獲取稿件中具有的tag , 以paperId為鍵,tagList為值的方式返回
	 * 
	 * @param paperIds 
	 * @return key 為 paperId , value 為tagList
	 */
	Map<Long, List<Tag>> groupTagsByPaperId(Collection<Long> paperIds);

	/**
	 * 根據 tagId 查詢與之有關的所有Paper
	 * 
	 * @param tagId
	 * @return
	 */
	List<Paper> getPaperByTagId(Long tagId);

	/**
	 * 根據 tagId 查詢與之有關的所有Paper關聯
	 * 
	 * @param tagId
	 * @return
	 */
	List<PaperTag> getPaperTagByTagId(Long tagId);

	/**
	 * 根據 tagIdSet 查詢與之有關的所有Paper關聯
	 * 
	 * @param tagIdList
	 * @return
	 */
	List<PaperTag> getPaperTagBytagIdList(List<Long> tagIdList);

	/**
	 * 為 稿件 新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param paperId
	 */
	void assignTagToPaper(List<Long> targetTagIdList, Long paperId);

	/**
	 * 為一個tag和paper新增關聯
	 * 
	 * @param paperTag
	 */
	void addPaperTag(PaperTag paperTag);

	/**
	 * 移除此 tag 與多篇 paper 關聯
	 * 
	 * @param tagId
	 * @param papersToRemove
	 */
	void removeTagRelationsForPapers(Long tagId, Set<Long> papersToRemove);
	
	/**
	 * 透過 paperId 和 tagId 建立關聯
	 * 
	 * @param paperId 稿件ID
	 * @param tagId 標籤ID
	 */
	void addPaperTag(Long paperId, Long tagId);
	
}
