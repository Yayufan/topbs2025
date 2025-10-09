package tw.com.topbs.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutArticleDTO;
import tw.com.topbs.pojo.entity.Article;

/**
 * <p>
 * 文章表 - 各個group的文章都儲存在這 服务类
 * </p>
 *
 * @author Joey
 * @since 2024-09-23
 */
public interface ArticleService extends IService<Article> {

	
	/**
	 * 獲取全部文章
	 * 
	 * @return
	 */
	List<Article> getAllArticle();
	
	/**
	 * 獲取全部文章(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<Article> getAllArticle(Page<Article> page);

	
	/**
	 * 獲取某個組別的所有文章
	 * 
	 * @param group
	 * @return
	 */
	List<Article> getAllArticleByGroup(String group);
	
	/**
	 * 給一般用戶使用，獲取某個組別的所有文章(分頁)<br>
	 * 會避開未到達發布日的資訊
	 * 
	 * @param group
	 * @param page
	 * @return
	 */
	IPage<Article> getAllArticleByGroup(String group,Page<Article> page);
	
	/**
	 * 給後台管理者使用，獲取某個組別的所有文章(分頁)<br>
	 * 會查詢到所有資訊
	 * 
	 * @param group
	 * @param page
	 * @return
	 */
	IPage<Article> getArticleByGroupForAdmin(String group,Page<Article> page);

	
	/**
	 * 獲取某個組別And類別的所有文章
	 * 
	 * @param category
	 * @return
	 */
	List<Article> getAllArticleByGroupAndCategory(String group,Long category);
	
	/**
	 * 獲取某個組別And類別的所有文章(分頁)
	 * 
	 * @param category
	 * @param page
	 * @return
	 */
	IPage<Article> getAllArticleByGroupAndCategory (String group,Long category,Page<Article> page);
	
	

	/**
	 * 獲取單一文章
	 * 
	 * @param articleId
	 * @return
	 */
	Article getArticle(Long articleId);
	
	

	/**
	 * 獲取單一文章，並增加該篇文章瀏覽量
	 * 
	 * @param articleId
	 * @return
	 */
	Article getShowArticle(Long articleId);

	/**
	 * 獲取文章總數
	 * 
	 * @return
	 */
	Long getArticleCount();
	
	/**
	 * 獲取某個組別的文章總數
	 * 
	 * @param group
	 * @return
	 */
	Long getArticleCountByGroup(String group);

	
	/**
	 * 獲取所有文章瀏覽量總數
	 * 
	 * @return
	 */
	Long getArticleViewsCount();

	
	/**
	 * 獲取某個組別所有文章瀏覽量總數
	 * 
	 * @param group
	 * @return
	 */
	Long getArticleViewsCountByGroup(String group);
	

	/**
	 * 新增文章
	 * 
	 * @param insertArticleDTO
	 */
	Long insertArticle(AddArticleDTO insertArticleDTO, MultipartFile[] files);

	/**
	 * 更新文章
	 * 
	 * @param updateArticleDTO
	 */
	void updateArticle(PutArticleDTO updateArticleDTO, MultipartFile[] files);

	/**
	 * 根據ArticleId刪除文章
	 * 
	 * @param articleId
	 */
	void deleteArticle(Long articleId);

	/**
	 * 批量刪除	 * 
	 * @param articleIdList
	 */
	void deleteArticle(List<Long> articleIdList);

	
	
}
