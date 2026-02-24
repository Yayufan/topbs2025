package tw.com.topbs.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.FormStatusEnum;
import tw.com.topbs.manager.FormManager;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormDTO;
import tw.com.topbs.pojo.VO.FormVO;
import tw.com.topbs.pojo.entity.Form;
import tw.com.topbs.service.FormService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 自定義客制化表單 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/form")
@RequiredArgsConstructor
public class FormController {

	private final FormService formService;
	private final FormManager formManager;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一表單")
	public R<Form> getForm(@PathVariable("id") Long formId) {
		Form form = formService.searchForm(formId);
		return R.ok(form);
	}

	@GetMapping("{id}/fill")
	@Operation(summary = "查詢 「可填寫的」 表單 , 包含表單欄位")
	public R<FormVO> getFillableForm(@PathVariable("id") Long formId) {
		FormVO formVO = formManager.getFillableForm(formId);
		return R.ok(formVO);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢表單分頁對象")
	public R<IPage<Form>> getFormPage(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String queryText,
			@RequestParam(required = false) @Schema(description = "可選值 draft,published,closed") String formStatus) {

		Page<Form> pageInfo = new Page<>(page, size);

		// 如果有傳值進來 , 進行轉換
		FormStatusEnum formStatusEnum = StringUtils.isNotBlank(formStatus) ? FormStatusEnum.fromValue(formStatus)
				: null;

		IPage<Form> formPage = formService.searchFormPageByQuery(pageInfo, formStatusEnum, queryText);
		return R.ok(formPage);
	}

	@PostMapping
	@Operation(summary = "新增單一表單", description = "補充:startTime 和 endTime 只要任一有填寫,另一個必須填寫；且 endTime 必須晚於 startTime")
	public R<Form> saveForm(@RequestBody @Valid AddFormDTO addFormDTO) {
		Form form = formService.create(addFormDTO);
		return R.ok(form);
	}

	@PutMapping
	@Operation(summary = "修改單一表單")
	public R<Void> updateForm(@RequestBody @Valid PutFormDTO putFormDTO) {
		formService.modify(putFormDTO);
		return R.ok();

	}

	@DeleteMapping("{id}")
	@Operation(summary = "刪除單一表單")
	public R<Void> deleteForm(@PathVariable("id") Long formId) {
		formService.removeById(formId);
		return R.ok();
	}

}
