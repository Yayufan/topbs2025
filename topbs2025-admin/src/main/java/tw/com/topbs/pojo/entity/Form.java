package tw.com.topbs.pojo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tw.com.topbs.enums.FormStatusEnum;

/**
 * <p>
 * 自定義客制化表單
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@Getter
@Setter
@TableName("form")
@Schema(name = "Form", description = "自定義客制化表單")
@ToString
public class Form implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("form_id")
	private Long formId;

	@Schema(description = "表單名稱")
	@TableField("title")
	private String title;

	@Schema(description = "表單描述")
	@TableField("description")
	private String description;

	@Schema(description = "draft(草稿),published(發佈),closed(關閉)")
	@TableField("status")
	private FormStatusEnum status;

	@Schema(description = "0為false(不需登入) , 1為true(需要登入)")
	@TableField("require_login")
	private Integer requireLogin;

	@Schema(description = "是否為簽退必填表單: 0為false(不是,簽退必填表單), 1為true(是,簽退必填表單)")
	@TableField("required_for_checkout")
	private Integer requiredForCheckout;

	@Schema(description = "0為false(不可重複填寫) , 1為true(可以重複填寫)")
	@TableField("allow_multiple_submissions")
	private Integer allowMultipleSubmissions;

	@Schema(description = "表單填寫開放時間")
	@TableField("start_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime startTime;

	@Schema(description = "表單填寫截止時間")
	@TableField("end_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime endTime;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField(value = "update_by", fill = FieldFill.UPDATE)
	private String updateBy;

	@Schema(description = "最後修改時間")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;
}
