package tw.com.topbs.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldOrderDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.FormField;
import tw.com.topbs.service.FormFieldService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/form-field")
@RequiredArgsConstructor
public class FormFieldController {

	private final FormFieldService formFieldService;

	@GetMapping("{id}")
	@Parameters({
		@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "根據表單ID 查詢表單內所有欄位")
	public R<List<FormFieldVO>> getFormFieldVOListByFormId(@PathVariable("id") Long formId) {
		List<FormFieldVO> formFieldVOList = formFieldService.searchFormStructureByForm(formId);
		return R.ok(formFieldVOList);
	}

	@PostMapping
	@Parameters({
		@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "新增單一表單欄位")
	public R<FormField> saveFormField(@RequestBody @Valid AddFormFieldDTO addFormFieldDTO) {
		FormField formField = formFieldService.add(addFormFieldDTO);
		return R.ok(formField);
	}
	
	@PostMapping("copy")
	@Parameters({
		@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "複製單一表單欄位")
	public R<FormField> copyFormField(@RequestBody @Valid AddFormFieldDTO addFormFieldDTO) {
		FormField formField = formFieldService.copy(addFormFieldDTO);
		return R.ok(formField);
	}

	@PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Parameters({
		@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "修改單一表單欄位", description = "請使用formData包裝,兩個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.files(value = array)<br>" + "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	public R<FormField> putFormField(@RequestPart(value = "file", required = false) MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = PutFormFieldDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {

		System.out.println("進行表單欄位修改");
		
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		PutFormFieldDTO putFormFieldDTO = objectMapper.readValue(jsonData, PutFormFieldDTO.class);

		// 更新表單欄位
		FormField formField = formFieldService.modify(file, putFormFieldDTO);

		return R.ok(formField);
	}
	
	@PutMapping("batch-order")
	@Parameters({
		@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "批量更新，表單欄位排序值")
	public R<Void> batchPutFormFieldOrder(@RequestBody @Valid List<PutFormFieldOrderDTO> putFormFieldOrderDTOList){
		
		// 如果裡面有元素則呼叫Service處理
		if(putFormFieldOrderDTOList.size() > 0) {
			formFieldService.batchModifyOrder(putFormFieldOrderDTOList);
		}
		
		//沒有則直接返回
		return R.ok();
	}
	

	@DeleteMapping("{id}")
	@Parameters({
		@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除單一表單欄位")
	public R<Void> deleteForm(@PathVariable("id") Long formFieldId) {
		System.out.println("刪除表單欄位");
		formFieldService.remove(formFieldId);
		return R.ok();
	}

}
