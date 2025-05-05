package tw.com.topbs.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleAttachmentDTO;
import tw.com.topbs.pojo.entity.ArticleAttachment;
import tw.com.topbs.service.ArticleAttachmentService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 文章附件的附件 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2024-12-27
 */
@Tag(name = "文章附件附件API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/article-attachment")
public class ArticleAttachmentController {

	private final ArticleAttachmentService articleAttachmentService;

	@GetMapping("{articleId}")
	@Operation(summary = "根據文章ID，查詢文章所有附件")
	public R<List<ArticleAttachment>> getAllArticleAttachment(@PathVariable("articleId") Long articleId) {
		List<ArticleAttachment> articleAttachmentList = articleAttachmentService
				.getAllArticleAttachmentByArticleId(articleId);
		return R.ok(articleAttachmentList);
	}

	@PostMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "data", description = "JSON 格式的附件資料", required = true, in = ParameterIn.QUERY, schema = @Schema(implementation = AddArticleAttachmentDTO.class)) })
	@SaCheckLogin
	@Operation(summary = "為某個文章新增附件")
	public R<Void> addArticleAttachment(@RequestParam("file") MultipartFile[] files,
			@RequestParam("data") String jsonData) throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		AddArticleAttachmentDTO insertArticleAttachmentDTO = objectMapper.readValue(jsonData,
				AddArticleAttachmentDTO.class);

		// 將檔案和資料對象傳給後端
		articleAttachmentService.insertArticleAttachment(insertArticleAttachmentDTO, files);

		return R.ok();
	}

	@DeleteMapping("{id}")
	@SaCheckLogin
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "根據 articleAttachment ID來刪除文章附件")
	public R<Void> deleteFile(@PathVariable("id") Long articleAttachmentId) {
		articleAttachmentService.deleteArticleAttachment(articleAttachmentId);
		return R.ok();
	}

}
