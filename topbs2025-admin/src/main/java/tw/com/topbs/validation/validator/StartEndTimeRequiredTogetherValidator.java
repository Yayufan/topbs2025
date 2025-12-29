package tw.com.topbs.validation.validator;

import java.time.LocalDateTime;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import tw.com.topbs.validation.annotation.ValidStartEndTimeRequiredTogether;
import tw.com.topbs.validation.constraint.HasStartEndTime;

public class StartEndTimeRequiredTogetherValidator
		implements ConstraintValidator<ValidStartEndTimeRequiredTogether, HasStartEndTime> {

	@Override
	public boolean isValid(HasStartEndTime target, ConstraintValidatorContext context) {
		if (target == null) {
			return true;
		}

		LocalDateTime start = target.getStartTime();
		LocalDateTime end = target.getEndTime();

		boolean startPresent = start != null;
		boolean endPresent = end != null;

		// 1️. 檢查是否同時存在
		// 當「兩者中只有一個有值」時，這個條件成立
		if (startPresent ^ endPresent) {
			// 停用預設錯誤訊息
			context.disableDefaultConstraintViolation();
			
			// 建立自訂錯誤訊息 , log 會看到的文字
			context.buildConstraintViolationWithTemplate("startTime 與 endTime 必須同時存在")
					// 指定錯誤對應欄位
					.addPropertyNode(startPresent ? "endTime" : "startTime")
					// 註冊錯誤並返回
					.addConstraintViolation();
			// 表示整個 DTO 驗證失敗
			return false;
		}

		// 2️. 檢查 startTime 是否晚於 endTime
		if (startPresent && endPresent && start.isAfter(end)) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("startTime 不可晚於 endTime")
					.addPropertyNode("startTime")
					.addConstraintViolation();
			return false;
		}

		// 如果都沒事,驗證成功
		return true;
	}
}
