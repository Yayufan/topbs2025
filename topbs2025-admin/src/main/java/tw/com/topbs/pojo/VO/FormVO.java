package tw.com.topbs.pojo.VO;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.enums.FormStatusEnum;

@Data
public class FormVO {

	@Schema(description = "主鍵ID")
	private Long formId;

	@Schema(description = "表單名稱")
	private String title;

	@Schema(description = "表單描述")
	private String description;

	@Schema(description = "draft(草稿),published(發佈),closed(關閉)")
	private FormStatusEnum status;

	@Schema(description = "0為false(不需登入) , 1為true(需要登入)")
	private Integer requireLogin;

	@Schema(description = "是否為簽退必填表單: 0為false(不是,簽退必填表單), 1為true(是,簽退必填表單)")
	private Integer requiredForCheckout;

	@Schema(description = "0為false(不可重複填寫) , 1為true(可以重複填寫)")
	@TableField("allow_multiple_submissions")
	private Integer allowMultipleSubmissions;

	@Schema(description = "表單填寫開放時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime startTime;

	@Schema(description = "表單填寫截止時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime endTime;
	
	@Schema(description = "表單的欄位題目")
	List<FormFieldVO> formFields;
	
}
