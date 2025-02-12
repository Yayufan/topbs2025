package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 系統設定表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("setting")
@Schema(name = "Setting", description = "系統設定表")
public class Setting implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("setting_id")
	private Long settingId;

	@Schema(description = "摘要投稿截止時間")
	@TableField("abstract_submission_end_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime abstractSubmissionEndTime;

	@Schema(description = "摘要開放投稿時間")
	@TableField("abstract_submission_start_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime abstractSubmissionStartTime;

	@Schema(description = "早鳥優惠_一階段截止時間")
	@TableField("early_bird_discount_phase_one_deadline")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime earlyBirdDiscountPhaseOneDeadline;

	@Schema(description = "早鳥優惠_三階段截止時間 (備用)")
	@TableField("early_bird_discount_phase_three_deadline")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime earlyBirdDiscountPhaseThreeDeadline;

	@Schema(description = "早鳥優惠_二階段截止時間 (備用)")
	@TableField("early_bird_discount_phase_two_deadline")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime earlyBirdDiscountPhaseTwoDeadline;

	@Schema(description = "最後下訂單 (訂房 or City Tour ) 時間")
	@TableField("last_order_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastOrderTime;

	@Schema(description = "最後註冊時間")
	@TableField("last_registration_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastRegistrationTime;

	@Schema(description = "Slide 上傳截止時間")
	@TableField("slide_upload_end_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime slideUploadEndTime;

	@Schema(description = "Slide 開放上傳時間")
	@TableField("slide_upload_start_time")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime slideUploadStartTime;

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
