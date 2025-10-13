package tw.com.topbs.controller;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerFileDTO;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 給審稿委員的公文檔案和額外]資料 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-06-03
 */

@Tag(name = "審稿委員公文附件API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paper-reviewer-file")
public class PaperReviewerFileController {

	private final PaperReviewerFileService paperReviewerFileService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "新增單一審稿委員公文", description = "請使用formData包裝,兩個key <br>"
			+ "1.paperReviewerId(value = paperReviewerId(string)<br>" + "2.檔案 file(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@SaCheckRole("super-admin")
	public R<PaperReviewerFile> savePaperReviewerFile(@RequestPart("file") @NotNull @Valid MultipartFile file,
			@RequestPart("paperReviewerId") @NotNull @Valid Long paperReviewerId) {
		paperReviewerFileService.addPaperReviewerFile(file, paperReviewerId);
		return R.ok();
	}

	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "修改單一審稿委員公文", description = "請使用formData包裝,兩個key <br>" + "1.data(value = DTO(json)<br>"
			+ "2.檔案 file(value = binary)<br>" + "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<PaperReviewerFile> updatePaperReviewerFile(@RequestPart("file") @NotNull @Valid MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = PutPaperReviewerFileDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		// 處理Java 8 LocalDate 和 LocalDateTime的轉換
		objectMapper.registerModule(new JavaTimeModule());
		// 正式轉換Json對象
		PutPaperReviewerFileDTO putPaperReviewerFileDTO = objectMapper.readValue(jsonData,
				PutPaperReviewerFileDTO.class);
		paperReviewerFileService.updatePaperReviewerFile(file, putPaperReviewerFileDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除稿件附件")
	@SaCheckRole("super-admin")
	public R<PaperReviewerFile> deletePaperReviewerFile(@PathVariable("id") Long reviewerFileId) {
		paperReviewerFileService.deleteReviewerFileById(reviewerFileId);
		return R.ok();
	}

}
