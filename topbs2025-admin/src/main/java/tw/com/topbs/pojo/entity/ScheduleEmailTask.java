package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 排程的電子郵件任務
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
@Getter
@Setter
@TableName("schedule_email_task")
@Schema(name = "ScheduleEmailTask", description = "排程的電子郵件任務")
@ToString
public class ScheduleEmailTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("schedule_email_task_id")
    private Long scheduleEmailTaskId;

    @Schema(description = "收信者類別,有member會員、attendees與會者、paper投稿者、paperReviewer審稿委員、")
    @TableField("recipient_category")
    private String recipientCategory;

    @Schema(description = "此排程信件任務的描述,用於補充")
    @TableField("description")
    private String description;

    @Schema(description = "任務開始時間")
    @TableField("start_time")
    private LocalDateTime startTime;

    @Schema(description = "信件主旨")
    @TableField("subject")
    private String subject;

    @Schema(description = "排程信件使用模板，用於儲存unlayer 的 design JSON數據")
    @TableField("design")
    private String design;

    @Schema(description = "排程信件使用模板，HTML 信件內容")
    @TableField("html_content")
    private String htmlContent;

    @Schema(description = "排程信件使用模板，當HTML 信件不支援時的 純文字內容")
    @TableField("plain_text")
    private String plainText;

    @Schema(description = "預期消耗的電子郵件數量")
    @TableField("expected_email_volume")
    private Integer expectedEmailVolume;

    @Schema(description = "任務狀態 , 0為pending、1為execute 、2為finished、3為failed、4為canceled")
    @TableField("status")
    private Integer status;

    @Schema(description = "創建時間")
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    @Schema(description = "創建者")
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @Schema(description = "最後更新時間")
    @TableField(value = "update_date", fill = FieldFill.UPDATE)
    private LocalDateTime updateDate;

    @Schema(description = "最後更新者")
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    @Schema(description = "邏輯刪除(0為存在,1為刪除)")
    @TableField("is_deleted")
    @TableLogic
    private String isDeleted;
}
