package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.FormField;

/**
 * <p>
 * 表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface FormFieldService extends IService<FormField> {

	/**
	 * 根據formId拿到所有蒐集欄位
	 * 
	 * @param formId
	 * @return
	 */
	List<FormFieldVO> getFormFieldsByFormId(Long formId);
	
	FormField addFormField(AddFormFieldDTO addFormFieldDTO);
	
	void updateFormField(PutFormFieldDTO putFormFieldDTO);
	
	void deleteFormField(Long formFieldId);
	
	
}
