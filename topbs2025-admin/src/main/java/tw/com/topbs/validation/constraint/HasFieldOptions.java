package tw.com.topbs.validation.constraint;

import tw.com.topbs.enums.FormFieldTypeEnum;
import tw.com.topbs.pojo.DTO.FormFieldOptionDTO;

public interface HasFieldOptions {

	public FormFieldTypeEnum getFieldType();
	
	public FormFieldOptionDTO getOptions();
	
}
