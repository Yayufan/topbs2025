package tw.com.topbs.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import tw.com.topbs.validation.validator.RequireLoginIfMultipleSubmissionsValidator;

@Documented
@Constraint(validatedBy = RequireLoginIfMultipleSubmissionsValidator.class)
@Target({ ElementType.TYPE })  // Class-level validation
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRequireLoginIfMultipleSubmissions {
	String message() default "當「重複填寫」設置開啟,「綁定登入狀態」也必須設置為開啟";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
