package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.ArticleAttachmentConvert;
import tw.com.topbs.mapper.ArticleAttachmentMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleAttachmentDTO;
import tw.com.topbs.pojo.entity.ArticleAttachment;
import tw.com.topbs.service.ArticleAttachmentService;
import tw.com.topbs.utils.MinioUtil;

/**
 * <p>
 * 文章的附件 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2024-12-27
 */
@Service
@RequiredArgsConstructor
public class ArticleAttachmentServiceImpl extends ServiceImpl<ArticleAttachmentMapper, ArticleAttachment>
		implements ArticleAttachmentService {

	private final ArticleAttachmentMapper articleAttachmentMapper;
	private final ArticleAttachmentConvert articleAttachmentConvert;
	private final MinioUtil minioUtil;
	private final String PATH = "articleAttachment";

	@Value("${minio.bucketName}")
	private String minioBucketName;

	@Override
	public List<ArticleAttachment> getAllArticleAttachmentByArticleId(Long articleId) {
		LambdaQueryWrapper<ArticleAttachment> articleAttachmentQueryWrapper = new LambdaQueryWrapper<>();
		articleAttachmentQueryWrapper.eq(ArticleAttachment::getArticleId, articleId);
		List<ArticleAttachment> articleAttachmentList = articleAttachmentMapper
				.selectList(articleAttachmentQueryWrapper);
		return articleAttachmentList;
	}

	@Override
	public IPage<ArticleAttachment> getAllArticleAttachmentByArticleId(Long articleId, Page<ArticleAttachment> page) {
		LambdaQueryWrapper<ArticleAttachment> articleAttachmentQueryWrapper = new LambdaQueryWrapper<>();
		articleAttachmentQueryWrapper.eq(ArticleAttachment::getArticleId, articleId);
		Page<ArticleAttachment> articleAttachmentPage = articleAttachmentMapper.selectPage(page,
				articleAttachmentQueryWrapper);
		return articleAttachmentPage;
	}

	@Override
	public void insertArticleAttachment(AddArticleAttachmentDTO insertArticleAttachmentDTO, MultipartFile[] files) {
		// 轉換檔案
		ArticleAttachment articleAttachment = articleAttachmentConvert.insertDTOToEntity(insertArticleAttachmentDTO);

		// 檔案存在，處理檔案
		if (files != null && files.length > 0) {

			List<String> upload = minioUtil.upload(minioBucketName, PATH + "/", files);
			// 基本上只有有一個檔案跟著formData上傳,所以這邊直接寫死,把唯一的url增添進對象中
			String url = upload.get(0);
			// 將bucketName 組裝進url
			url = "/" + minioBucketName + "/" + url;
			// minio完整路徑放路對象中
			articleAttachment.setPath(url);

			// 放入資料庫
			baseMapper.insert(articleAttachment);

		}

		System.out.println("上傳完成");

	}

	@Override
	public void deleteArticleAttachment(Long articleAttachmentId) {

		ArticleAttachment articleAttachment = articleAttachmentMapper.selectById(articleAttachmentId);

		String filePath = articleAttachment.getPath();
		String result = filePath.substring(filePath.indexOf("/", 1));

		// 透過Minio進行刪除
		minioUtil.removeObject(minioBucketName, result);
		// 資料庫資料刪除
		baseMapper.deleteById(articleAttachmentId);

	}

}
