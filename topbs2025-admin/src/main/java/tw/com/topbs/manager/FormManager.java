package tw.com.topbs.manager;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormConvert;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.VO.FormVO;
import tw.com.topbs.pojo.entity.Form;
import tw.com.topbs.service.FormFieldService;
import tw.com.topbs.service.FormService;

/**
 * 
 */
@Component
@RequiredArgsConstructor
public class FormManager {

	private final FormConvert formConvert;
	
	private final FormService formService;
	private final FormFieldService formFieldService;
	
	
	/**
	 * 
	 * @param formId
	 * @return
	 */
	public FormVO getFillableForm(Long formId) {
		
		// 1.查詢要填寫的表單
		Form form = formService.getForm(formId);
		
		// 2.轉換資料
		FormVO formVO = formConvert.entityToVO(form);
		
		// 3.根據 formId 查詢表單 及其 欄位
		List<FormFieldVO> formFieldVOList = formFieldService.getFormFieldsByFormId(formId);
		
		// 4.VO填充欄位
		formVO.setFormFields(formFieldVOList);
		
		return formVO;
	}
	

	
}
