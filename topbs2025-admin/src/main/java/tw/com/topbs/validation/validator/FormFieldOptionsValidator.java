package tw.com.topbs.validation.validator;

import java.util.EnumSet;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tw.com.topbs.enums.FormFieldTypeEnum;
import tw.com.topbs.pojo.DTO.FormFieldOptionDTO;
import tw.com.topbs.validation.annotation.ValidFormFieldOptions;
import tw.com.topbs.validation.constraint.HasFieldOptions;

public class FormFieldOptionsValidator implements ConstraintValidator<ValidFormFieldOptions, HasFieldOptions> {

	// 拿到允許使用options 的 fieldType
	private static final EnumSet<FormFieldTypeEnum> OPTION_REQUIRED_TYPES = EnumSet.of(FormFieldTypeEnum.SELECT,
			FormFieldTypeEnum.RADIO, FormFieldTypeEnum.RATE, FormFieldTypeEnum.CHECKBOX);

	@Override
	public boolean isValid(HasFieldOptions target, ConstraintValidatorContext context) {

		if (target == null) {
			return true;
		}

		FormFieldTypeEnum fieldType = target.getFieldType();
		FormFieldOptionDTO options = target.getOptions();

		// fieldType 為 null 的情況交由 @NotNull / enum 驗證處理
		if (fieldType == null) {
			return true;
		}

		boolean requiresOptions = OPTION_REQUIRED_TYPES.contains(fieldType);

		if (requiresOptions && options == null) {
			return buildViolation(context, "當前 fieldType 缺少options對象", "options");
		}

		if (!requiresOptions && options != null) {
			return buildViolation(context, "當前 fieldType 不允許持有 options 對象", "options");
		}

		// 表示DTO驗證成功
		return true;
	}

	/**
	 * 
	 * @param context
	 * @param messageKey
	 * @param property
	 * @return
	 */
	private boolean buildViolation(ConstraintValidatorContext context, String messageKey, String property) {
		// 停用預設錯誤訊息
		context.disableDefaultConstraintViolation();
		// 建立自訂錯誤訊息 , log 會看到的文字
		context.buildConstraintViolationWithTemplate(messageKey)
				// 指定錯誤對應欄位
				.addPropertyNode(property)
				// 註冊錯誤並返回
				.addConstraintViolation();

		// 表示整個 DTO 驗證失敗
		return false;
	}
}