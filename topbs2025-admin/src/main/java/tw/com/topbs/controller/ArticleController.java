package tw.com.topbs.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddArticleDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutArticleDTO;
import tw.com.topbs.pojo.entity.Article;
import tw.com.topbs.service.ArticleService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 文章表 - 各個group的文章都儲存在這 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2024-09-23
 */

@Tag(name = "文章API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/article")
public class ArticleController {

	private final ArticleService articleService;

	@GetMapping("group/{id}")
	@Operation(summary = "查詢單一文章(For管理後台)")
	public R<Article> getArticle(@PathVariable("id") Long articleId) {
		Article article = articleService.getArticle(articleId);
		return R.ok(article);
	}

	@GetMapping("show/{id}")
	@Operation(summary = "查詢單一文章(For形象頁面,增加瀏覽量)")
	public R<Article> getShowArticle(@PathVariable("id") Long articleId) {
		Article article = articleService.getShowArticle(articleId);
		return R.ok(article);
	}

	@GetMapping
	@Operation(summary = "查詢所有文章")
	public R<List<Article>> getArticleList() {
		List<Article> articleList = articleService.getArticleList();
		return R.ok(articleList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢所有文章(分頁)")
	public R<IPage<Article>> getArticlePage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Article> pageInfo = new Page<>(page, size);
		IPage<Article> articleList = articleService.getArticlePage(pageInfo);

		return R.ok(articleList);
	}

	@GetMapping("{group}")
	@Operation(summary = "查詢某個組別所有文章")
	public R<List<Article>> getArticleListByGroup(@PathVariable("group") String group) {
		List<Article> articleList = articleService.getArticleListByGroup(group);
		return R.ok(articleList);
	}

	@GetMapping("{group}/pagination")
	@Operation(summary = "查詢某個組別所有文章(分頁)，For 一般用戶，隱藏未發布的文章")
	public R<IPage<Article>> getArticlePageByGroup(@PathVariable("group") String group, @RequestParam Integer page,
			@RequestParam Integer size) {
		Page<Article> pageInfo = new Page<>(page, size);
		IPage<Article> articleList = articleService.getArticlePageByGroup(group, pageInfo);
		return R.ok(articleList);
	}

	@GetMapping("admin/{group}/pagination")
	@Operation(summary = "查詢某個組別所有文章(分頁)，For Admin ，顯示所有文章")
	@SaCheckRole("super-admin")
	public R<IPage<Article>> getArticlePageByGroupForAdmin(@PathVariable("group") String group,
			@RequestParam Integer page, @RequestParam Integer size) {
		Page<Article> pageInfo = new Page<>(page, size);
		IPage<Article> articleList = articleService.getArticlePageByGroupForAdmin(group, pageInfo);
		return R.ok(articleList);
	}

	@GetMapping("{group}/{catrgory}")
	@Operation(summary = "查詢某個組別and類別所有文章")
	public R<List<Article>> getArticleListByCategory(@PathVariable("group") String group,
			@PathVariable("catrgory") Long catrgory) {
		List<Article> articleList = articleService.getArticleListByGroupAndCategory(group, catrgory);
		return R.ok(articleList);
	}

	@GetMapping("{group}/{catrgory}/pagination")
	@Operation(summary = "查詢某個組別and類別所有文章(分頁)")
	public R<IPage<Article>> getArticlePageByCategory(@PathVariable("group") String group,
			@PathVariable("catrgory") Long catrgory, @RequestParam Integer page, @RequestParam Integer size) {
		Page<Article> pageInfo = new Page<>(page, size);
		IPage<Article> articleList = articleService.getArticlePageByGroupAndCategory(group, catrgory, pageInfo);
		return R.ok(articleList);
	}

	@GetMapping("count")
	@Operation(summary = "查詢所有文章總數")
	public R<Long> getArticleCount() {
		Long articleCount = articleService.getArticleCount();
		return R.ok(articleCount);
	}

	@GetMapping("{group}/count")
	@Operation(summary = "查詢某組別的文章總數")
	public R<Long> getArticleCount(@PathVariable("group") String group) {
		Long articleCount = articleService.getArticleCountByGroup(group);
		return R.ok(articleCount);
	}

	@GetMapping("views-count")
	@Operation(summary = "查詢所有文章的瀏覽總數")
	public R<Long> getArticleViewsCount() {
		Long articleCount = articleService.getArticleViewsCount();
		return R.ok(articleCount);
	}

	@GetMapping("{group}/views-count")
	@Operation(summary = "查詢某組別文章瀏覽量總數")
	public R<Long> getArticleViewsCount(@PathVariable("group") String group) {
		Long articleCount = articleService.getArticleViewsCountByGroup(group);
		return R.ok(articleCount);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "新增文章", description = "請使用formData包裝,兩個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.縮略圖 file(value = binary)<br>" + "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	public R<Long> saveArticle(@RequestPart(value = "file", required = false) MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = AddArticleDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		// 處理Java 8 LocalDate 和 LocalDateTime的轉換
		objectMapper.registerModule(new JavaTimeModule());

		AddArticleDTO addArticleDTO = objectMapper.readValue(jsonData, AddArticleDTO.class);

		Long articleId = articleService.insertArticle(addArticleDTO, file);
		return R.ok(articleId);

	}

	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "更新文章", description = "請使用formData包裝,兩個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.縮略圖 file(value = binary)<br>" + "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	public R<Void> updateArticle(@RequestPart(value = "file", required = false) MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = PutArticleDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		// 處理Java 8 LocalDate 和 LocalDateTime的轉換
		objectMapper.registerModule(new JavaTimeModule());

		PutArticleDTO putArticleDTO = objectMapper.readValue(jsonData, PutArticleDTO.class);

		articleService.updateArticle(putArticleDTO, file);
		return R.ok();

	}

	@Operation(summary = "刪除文章")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@DeleteMapping("{id}")
	public R<Void> deleteArticle(@PathVariable("id") Long articleId) {
		articleService.deleteArticle(articleId);
		return R.ok();

	}

	@Operation(summary = "批量刪除文章")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@DeleteMapping()
	public R<Void> batchDeleteArticle(@Valid @NotNull @RequestBody List<Long> articleIdList) {
		articleService.deleteArticle(articleIdList);
		return R.ok();
	}

}
