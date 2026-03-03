package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tw.com.topbs.enums.CommonStatusEnum;
import tw.com.topbs.enums.FormStatusEnum;
import tw.com.topbs.validation.annotation.ValidRequireLoginIfMultipleSubmissions;
import tw.com.topbs.validation.annotation.ValidStartEndTimeRequiredTogether;
import tw.com.topbs.validation.constraint.HasLoginAndMultipleSubmissionRules;
import tw.com.topbs.validation.constraint.HasStartEndTime;

@Data
@ValidStartEndTimeRequiredTogether
@ValidRequireLoginIfMultipleSubmissions
public class PutFormDTO  implements HasStartEndTime,HasLoginAndMultipleSubmissionRules {

	@Schema(description = "主鍵ID")
	private Long formId;

	@Schema(description = "表單名稱")
	@NotBlank
	private String title;

	@Schema(description = "表單描述")
	private String description;

	@Schema(description = "draft(草稿),published(發佈),closed(關閉)")
	@NotNull
	private FormStatusEnum status;

	@Schema(description = "0為false(不需登入) , 1為true(需要登入)")
	@NotNull
	private CommonStatusEnum requireLogin;
	
	@Schema(description = "0為false(不可重複填寫) , 1為true(可以重複填寫)")
	@NotNull
	private CommonStatusEnum allowMultipleSubmissions;

	@Schema(description = "是否為簽退必填表單: 0為false(不是,簽退必填表單), 1為true(是,簽退必填表單)")
	@NotNull
	private CommonStatusEnum requiredForCheckout;

	@Schema(description = "表單填寫開放時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime startTime;

	@Schema(description = "表單填寫截止時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime endTime;

}
