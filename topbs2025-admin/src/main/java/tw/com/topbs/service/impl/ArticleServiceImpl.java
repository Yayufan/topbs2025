package tw.com.topbs.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.ArticleConvert;
import tw.com.topbs.mapper.ArticleMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutArticleDTO;
import tw.com.topbs.pojo.entity.Article;
import tw.com.topbs.service.ArticleService;
import tw.com.topbs.service.CmsService;
import tw.com.topbs.utils.ArticleViewsCounterUtil;
import tw.com.topbs.utils.MinioUtil;

/**
 * <p>
 * 文章表 - 各個group的文章都儲存在這 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2024-09-23
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

	private static final String DEFAULT_IMAGE_PATH = "/topbs2025/default-image/cta-img-1.jpg";

	@Value("${minio.bucketName}")
	private String minioBucketName;

	private final ArticleViewsCounterUtil articleViewsCounterUtil;
	private final MinioUtil minioUtil;
	private final ArticleConvert articleConvert;
	private final CmsService cmsService;

	@Override
	public List<Article> getArticleList() {
		List<Article> articleList = baseMapper.selectList(null);
		return articleList;
	}

	@Override
	public IPage<Article> getArticlePage(Page<Article> page) {
		Page<Article> articleList = baseMapper.selectPage(page, null);
		return articleList;
	}

	@Override
	public List<Article> getArticleListByGroup(String group) {
		LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
		articleQueryWrapper.eq(Article::getGroupType, group);

		List<Article> articleList = baseMapper.selectList(articleQueryWrapper);
		return articleList;
	}

	@Override
	public IPage<Article> getArticlePageByGroup(String group, Page<Article> page) {
		// 查詢群組、分頁，並倒序排列

		// 查詢今天
		LocalDate today = LocalDate.now();

		// 查詢群組、分頁，發布日是今天以前的文章，並根據發布日期倒序排列
		LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
		articleQueryWrapper.eq(Article::getGroupType, group).orderByDesc(Article::getArticleId);
		articleQueryWrapper.eq(Article::getGroupType, group)
				.le(Article::getAnnouncementDate, today)
				.orderByDesc(Article::getAnnouncementDate);
		Page<Article> articleList = baseMapper.selectPage(page, articleQueryWrapper);

		return articleList;
	}
	
	@Override
	public IPage<Article> getArticlePageByGroupForAdmin(String group, Page<Article> page) {

		// 查詢群組、分頁，並根據ID倒序排列
		LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
		articleQueryWrapper.eq(Article::getGroupType, group)
				.orderByDesc(Article::getArticleId);
		Page<Article> articleList = baseMapper.selectPage(page, articleQueryWrapper);

		return articleList;
	}

	@Override
	public List<Article> getArticleListByGroupAndCategory(String group, Long category) {
		LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
		articleQueryWrapper.eq(Article::getGroupType, group).eq(Article::getCategoryId, category);

		List<Article> articleList = baseMapper.selectList(articleQueryWrapper);
		return articleList;
	}

	@Override
	public IPage<Article> getArticlePageByGroupAndCategory(String group, Long category, Page<Article> page) {
		// 查詢群組、分頁，並倒序排列
		LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
		articleQueryWrapper.eq(Article::getGroupType, group).eq(Article::getCategoryId, category)
				.orderByDesc(Article::getArticleId);
		Page<Article> articleList = baseMapper.selectPage(page, articleQueryWrapper);

		return articleList;
	}

	@Override
	public Article getArticle(Long articleId) {
		return baseMapper.selectById(articleId);
	}

	@Override
	public Article getShowArticle(Long articleId) {
		Article article = baseMapper.selectById(articleId);
		articleViewsCounterUtil.incrementViewCount(article.getGroupType(), articleId);
		return article;
	}

	@Override
	public Long getArticleCount() {
		return baseMapper.selectCount(null);
	}

	@Override
	public Long getArticleCountByGroup(String group) {
		LambdaQueryWrapper<Article> articleQueryWrapper = new LambdaQueryWrapper<>();
		articleQueryWrapper.eq(Article::getGroupType, group);
		return baseMapper.selectCount(articleQueryWrapper);

	}

	@Override
	public Long getArticleViewsCountByGroup(String group) {
		return articleViewsCounterUtil.getTotalViewCount(group);
	}

	@Override
	public Long insertArticle(AddArticleDTO addArticleDTO, MultipartFile[] files) {
		Article article = articleConvert.addDTOToEntity(addArticleDTO);

		// 檔案存在，處理檔案
		if (files != null && files.length > 0) {

			List<String> upload = minioUtil.upload(minioBucketName, article.getGroupType() + "/", files);
			// 基本上只有有一個檔案跟著formData上傳,所以這邊直接寫死,把唯一的url增添進對象中
			String url = upload.get(0);
			// 將bucketName 組裝進url
			url = "/" + minioBucketName + "/" + url;
			// minio完整路徑放路對象中
			article.setCoverThumbnailUrl(url);
			// 放入資料庫
			baseMapper.insert(article);

		} else {
			// 沒有檔案,直接處理數據
			// 將類別名稱放入對象中
			article.setCoverThumbnailUrl(DEFAULT_IMAGE_PATH);
			baseMapper.insert(article);
		}

		// 最後都返回自增ID
		return article.getArticleId();

	}

	@Override
	public void updateArticle(PutArticleDTO putArticleDTO, MultipartFile[] files) {

		// 先拿到舊的資料
		Article originalArticle = baseMapper.selectById(putArticleDTO.getArticleId());

		// 拿到本次資料
		Article article = articleConvert.putDTOToEntity(putArticleDTO);

		// 獲取當前頁面有上傳過的圖片URL網址
		List<String> tempUploadUrl = putArticleDTO.getTempUploadUrl();

		// 獲取本次資料傳來的HTML字符串
		String newContent = article.getContent();

		// 獲得舊的資料的HTML字符串
		String oldContent = originalArticle.getContent();

		// 先判斷這個要修改的文章,他是不是關聯其他文章，兩邊最大的不同就是對檔案的處理

		// 檔案存在，處理檔案
		if (files != null && files.length > 0) {

			// 獲取之前的縮圖,並刪除之前的圖檔
			String coverThumbnailUrl = originalArticle.getCoverThumbnailUrl();

			// 因為縮圖圖檔URL有包含 scuro, 這邊先進行截斷
			String result = coverThumbnailUrl.substring(coverThumbnailUrl.indexOf("/", 1));

			// 如果原縮圖不為預設值,圖片進行刪除
			if (!coverThumbnailUrl.equals(DEFAULT_IMAGE_PATH)) {
				minioUtil.removeObject(minioBucketName, result);
			}

			// 上傳檔案
			List<String> upload = minioUtil.upload(minioBucketName, article.getGroupType() + "/", files);
			// 基本上只有有一個檔案跟著formData上傳,所以這邊直接寫死,把唯一的url增添進對象中
			String url = upload.get(0);
			// 將bucketName 組裝進url
			url = "/" + minioBucketName + "/" + url;
			// minio完整路徑放路對象中
			article.setCoverThumbnailUrl(url);
		}

		// 最後移除舊的無使用的圖片以及臨時的圖片路徑
		cmsService.cleanNotUsedImg(newContent, oldContent, tempUploadUrl, minioBucketName);

		// 更新數據
		baseMapper.updateById(article);

	}

	@Override
	public void deleteArticle(Long articleId) {
		Article article = baseMapper.selectById(articleId);

		// 刪除自身資料
		// 如果縮圖不為預設值,圖片才進行刪除
		if (!article.getCoverThumbnailUrl().equals(DEFAULT_IMAGE_PATH)) {
			List<String> list = new ArrayList<>();
			list.add(article.getCoverThumbnailUrl());
			List<String> paths = minioUtil.extractPaths(minioBucketName, list);
			minioUtil.removeObjects(minioBucketName, paths);
		}

		// 刪除資料前,刪除對應的內容圖片檔案
		cmsService.cleanNotUsedImg(article.getContent(), minioBucketName);

		// 刪除資料
		baseMapper.deleteById(article.getArticleId());

	}

	@Override
	public void deleteArticle(List<Long> articleIdList) {
		// 遍歷循環刪除
		for (Long articleId : articleIdList) {
			// 去執行單個刪除
			deleteArticle(articleId);

		}

	}

	@Override
	public Long getArticleViewsCount() {
		// TODO Auto-generated method stub
		return null;
	}

}
