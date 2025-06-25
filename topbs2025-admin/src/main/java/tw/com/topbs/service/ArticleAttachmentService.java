package tw.com.topbs.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleAttachmentDTO;
import tw.com.topbs.pojo.entity.ArticleAttachment;

/**
 * <p>
 * 文章的附件 服务类
 * </p>
 *
 * @author Joey
 * @since 2024-12-27
 */
public interface ArticleAttachmentService extends IService<ArticleAttachment> {

	/**
	 * 根據文章ID,獲取該文章的所有附件
	 * 
	 * @param articleId
	 * @return
	 */
	List<ArticleAttachment> getAllArticleAttachmentByArticleId(Long articleId);

	/**
	 * 根據文章ID,獲取該文章的所有附件(分頁)
	 * 
	 * @param articleId
	 * @param page
	 * @return
	 */
	IPage<ArticleAttachment> getAllArticleAttachmentByArticleId(Long articleId, Page<ArticleAttachment> page);

	
	/**
	 * 新增文章附件
	 * 
	 * @param addArticleAttachmentDTO
	 * @param file
	 */
	void addArticleAttachment(AddArticleAttachmentDTO addArticleAttachmentDTO, MultipartFile file);

	/**
	 * 根據articleAttachmentId刪除文章附件
	 * 
	 * @param articleAttachmentId
	 */
	void deleteArticleAttachment(Long articleAttachmentId);


}
