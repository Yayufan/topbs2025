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
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPublishFileDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPublishFileDTO;
import tw.com.topbs.pojo.entity.PublishFile;
import tw.com.topbs.service.PublishFileService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 發佈檔案表 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Tag(name = "檔案中心API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/publish-file")
public class PublishFileController {

	private final PublishFileService publishFileService;

	@GetMapping("{group}")
	@Operation(summary = "查詢某個組別 及 類別的所有檔案")
	public R<List<PublishFile>> getPublishFileListByGroup(@PathVariable("group") String group,
			@RequestParam String type) {
		List<PublishFile> fileList = publishFileService.getFileListByGroupAndType(group, type);
		return R.ok(fileList);
	}

	@GetMapping("{group}/pagination")
	@Operation(summary = "查詢某個組別所有檔案 (分頁)")
	public R<IPage<PublishFile>> getPublishFilePageByGroup(@PathVariable("group") String group,
			@RequestParam Integer page, @RequestParam Integer size) {
		Page<PublishFile> pageInfo = new Page<>(page, size);
		IPage<PublishFile> fileList = publishFileService.getFilePageByGroup(group, pageInfo);
		return R.ok(fileList);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "新增檔案", description = "請使用formData包裝,三個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.檔案 file(value = binary)<br>" + "3.縮圖檔案 imgFile(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> addPublishFile(@RequestPart("file") MultipartFile file,
			@RequestPart(value = "imgFile", required = false) MultipartFile imgFile,
			@RequestPart("data") @Schema(name = "data", implementation = AddPublishFileDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		AddPublishFileDTO addPublishFileDTO = objectMapper.readValue(jsonData, AddPublishFileDTO.class);

		// 將檔案和資料對象傳給後端
		publishFileService.addPublishFile(file, imgFile, addPublishFileDTO);

		return R.ok();
	}

	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "修改檔案", description = "請使用formData包裝,三個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.檔案 file(value = binary)<br>" + "3.縮圖檔案 imgFile(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> putPublishFile(@RequestPart(value = "file", required = false) MultipartFile file,
			@RequestPart(value = "imgFile", required = false) MultipartFile imgFile,
			@RequestPart("data") @Schema(name = "data", implementation = PutPublishFileDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		PutPublishFileDTO putPublishFileDTO = objectMapper.readValue(jsonData, PutPublishFileDTO.class);

		// 將檔案和資料對象傳給後端
		publishFileService.putPublishFile(file, imgFile, putPublishFileDTO);

		return R.ok();
	}

	@DeleteMapping("{id}")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "根據ID來刪除檔案")
	public R<Void> deletePublishFile(@PathVariable("id") Long publishFileId) {
		publishFileService.deletePublishFile(publishFileId);
		return R.ok();
	}

	@Operation(summary = "批量刪除檔案")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@DeleteMapping
	public R<Void> batchDeletePublishFile(@Valid @NotNull @RequestBody List<Long> publishFileIdList) {
		publishFileService.deletePublishFile(publishFileIdList);
		return R.ok();
	}

}
