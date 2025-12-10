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
import tw.com.topbs.utils.S3Util;

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

	private final ArticleAttachmentConvert articleAttachmentConvert;
	private final S3Util s3Util;
	private final String PATH = "article-attachment";

	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	@Override
	public List<ArticleAttachment> getAllArticleAttachmentByArticleId(Long articleId) {
		LambdaQueryWrapper<ArticleAttachment> articleAttachmentQueryWrapper = new LambdaQueryWrapper<>();
		articleAttachmentQueryWrapper.eq(ArticleAttachment::getArticleId, articleId);
		List<ArticleAttachment> articleAttachmentList = baseMapper.selectList(articleAttachmentQueryWrapper);
		return articleAttachmentList;
	}

	@Override
	public IPage<ArticleAttachment> getAllArticleAttachmentByArticleId(Long articleId, Page<ArticleAttachment> page) {
		LambdaQueryWrapper<ArticleAttachment> articleAttachmentQueryWrapper = new LambdaQueryWrapper<>();
		articleAttachmentQueryWrapper.eq(ArticleAttachment::getArticleId, articleId);
		Page<ArticleAttachment> articleAttachmentPage = baseMapper.selectPage(page, articleAttachmentQueryWrapper);
		return articleAttachmentPage;
	}

	@Override
	public void addArticleAttachment(AddArticleAttachmentDTO addArticleAttachmentDTO, MultipartFile file) {
		// 1.轉換檔案
		ArticleAttachment articleAttachment = articleAttachmentConvert.addDTOToEntity(addArticleAttachmentDTO);

		// 2.Controller 層較驗過了，檔案必定存在，處理檔案
		String dbUrl = s3Util.upload(PATH, addArticleAttachmentDTO.getName(), file);
		articleAttachment.setPath(dbUrl);

		// 3.放入資料庫
		baseMapper.insert(articleAttachment);

	}

	@Override
	public void deleteArticleAttachment(Long articleAttachmentId) {

		ArticleAttachment articleAttachment = baseMapper.selectById(articleAttachmentId);

		String filePath = articleAttachment.getPath();
		String s3Key = s3Util.extractS3PathInDbUrl(bucketName, filePath);

		// 在S3進行刪除
		s3Util.removeFile(bucketName, s3Key);

		// 資料庫資料刪除
		baseMapper.deleteById(articleAttachmentId);

	}

}
