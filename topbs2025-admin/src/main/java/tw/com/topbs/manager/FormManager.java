package tw.com.topbs.manager;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormConvert;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.VO.FormVO;
import tw.com.topbs.pojo.entity.Form;
import tw.com.topbs.pojo.entity.FormResponse;
import tw.com.topbs.service.FormFieldService;
import tw.com.topbs.service.FormResponseService;
import tw.com.topbs.service.FormService;
import tw.com.topbs.service.ResponseAnswerService;

/**
 * 
 */
@Component
@RequiredArgsConstructor
public class FormManager {

	private final FormConvert formConvert;
	private final FormService formService;
	private final FormFieldService formFieldService;
	private final FormResponseService formResponseService;
	private final ResponseAnswerService responseAnswerService;

	/**
	 * 
	 * @param formId
	 * @return
	 */
	public FormVO getFillableForm(Long formId) {

		// 1.查詢要填寫的表單
		Form form = formService.searchForm(formId);

		// 2.轉換資料
		FormVO formVO = formConvert.entityToVO(form);

		// 3.根據 formId 查詢表單 及其 欄位
		List<FormFieldVO> formFieldVOList = formFieldService.searchFormStructureByForm(formId);

		// 4.VO填充欄位
		formVO.setFormFields(formFieldVOList);

		return formVO;
	}

	
	public void deleteForm(Long formId) {
		// 1.透過 formId 查詢關於這份表單的所有回覆 ， 並提取responseIds
		List<FormResponse> formResponses = formResponseService.searchSubmissionsByForm(formId);
		List<Long> responseIds = formResponses.stream().map(FormResponse::getFormResponseId).toList();
		
		// 2.根據 responseIds 刪除所有answer
		responseAnswerService.removeByResponses(responseIds);
		
		// 3.根據formId 刪除所有 response
		formResponseService.removeByForm(formId);
		
		// 4.刪除表單本身
		formService.removeById(formId);
		
	}
	

}
