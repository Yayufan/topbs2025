package tw.com.topbs.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.manager.FormResponseManager;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormResponseDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutResponseAnswerDTO;
import tw.com.topbs.pojo.VO.FormVO;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 表單回覆紀錄 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@RestController
@RequestMapping("/form-response")
@RequiredArgsConstructor
public class FormResponseController {

	private final FormResponseManager formResponseManager;

	@GetMapping("{id}")
	@Operation(summary = "查詢 「要修改」 表單回覆 , 包含表單欄位 及 之前填寫數據")
	public R<FormVO> getEditableForm(@PathVariable("id") Long responseId) {
		return R.ok(formResponseManager.getEditableForm(responseId));
	}

	@PostMapping
	@Operation(summary = "新增單一表單回覆")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = false, in = ParameterIn.HEADER) })
	public R<Void> saveFormResponse(@RequestBody @Valid AddFormResponseDTO formResponseDTO) {

		// 1.初始化memberId
		Long memberId = null;

		// 2.如果有傳token , 且是有在Redis中紀錄的登入狀態,會拿到loginId , 業務上來說也是memberId
		Object loginId = StpKit.MEMBER.getLoginIdDefaultNull();
		if (loginId != null) {
			memberId = Long.valueOf(loginId.toString());
		}

		// 3.不論memberId是否有值,都放進DTO中
		formResponseDTO.setMemberId(memberId);

		// 4.調用表單回覆新增
		formResponseManager.addFormResponse(formResponseDTO);

		return R.ok();
	}

	@PutMapping
	@Operation(summary = "修改 單一表單回覆，只給管理者修改")
	public R<Void> updateFormResponse(
			@RequestBody @Valid @Size(min = 1) List<PutResponseAnswerDTO> putResponseAnswerDTOList) {
		formResponseManager.updateFormResponse(putResponseAnswerDTOList);
		return R.ok();

	}

	@DeleteMapping("{id}")
	@Operation(summary = "刪除 單一表單回覆，只給管理者刪除")
	public R<Void> deleteFormResponse(@PathVariable("id") Long formResponseId) {
		formResponseManager.deleteFormResponse(formResponseId);
		return R.ok();
	}

	
	@PostMapping("download-excel")
	@Operation(summary = "下載某個表單的 Excel 所有回覆")
	public R<Void> downloadFormResponseExcel(HttpServletResponse response, Long formId) throws IOException {
		formResponseManager.downloadFormResponseExcel(response, formId);
		return R.ok();
	}

}
