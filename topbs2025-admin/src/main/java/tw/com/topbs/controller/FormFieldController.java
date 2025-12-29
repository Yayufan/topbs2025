package tw.com.topbs.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.Form;
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
	@Operation(summary = "根據表單ID 查詢表單內所有欄位")
	public R<List<FormFieldVO>> getFormFieldVOListByFormId(@PathVariable("id") Long formId) {
		List<FormFieldVO> formFieldVOList = formFieldService.getFormFieldsByFormId(formId);
		return R.ok(formFieldVOList);
	}
	
	@PostMapping
	@Operation(summary = "新增單一表單欄位")
	public R<FormField> saveForm(@RequestBody @Valid AddFormFieldDTO addFormFieldDTO) {
		FormField formField = formFieldService.addFormField(addFormFieldDTO);
		return R.ok(formField);
	}
	
	
	
}
