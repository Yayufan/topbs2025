package tw.com.topbs.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import tw.com.topbs.validation.validator.FormFieldOptionsValidator;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// 綁定Validation框架 的 約束器,並指定自定義約束 FormFieldOptionsValidator.class
@Constraint(validatedBy = FormFieldOptionsValidator.class)
@Documented
public @interface ValidFormFieldOptions {
	String message() default "Field Option 驗證不合規";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
