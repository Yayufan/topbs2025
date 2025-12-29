package tw.com.topbs.validation.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import tw.com.topbs.validation.validator.StartEndTimeRequiredTogetherValidator;

// 只能用在 class 或 interface
@Target(ElementType.TYPE)
// 運行時仍保留 annotation
@Retention(RetentionPolicy.RUNTIME)
// 告訴 Bean Validation framework 這個 annotation 對應的驗證邏輯在哪裡
@Constraint(validatedBy = StartEndTimeRequiredTogetherValidator.class)
// 文件化
@Documented
public @interface ValidStartEndTimeRequiredTogether {

	// 驗證失敗時的預設訊息（可以被覆寫或用於 i18n）
	String message() default "startTime 與 endTime 必須同時存在或同時為空";

	// Bean Validation 的 分組驗證，例如不同場景（新增/更新）可套用不同規則
	Class<?>[] groups() default {};

	// 可自定義 metadata（通常不常用，屬於 Bean Validation 標準）
	Class<? extends Payload>[] payload() default {};
}