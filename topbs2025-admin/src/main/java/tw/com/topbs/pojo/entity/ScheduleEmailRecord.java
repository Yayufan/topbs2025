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

/**
 * <p>
 * 排程寄信任務的收信者,及信件內容
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
@Getter
@Setter
@TableName("schedule_email_record")
@Schema(name = "ScheduleEmailRecord", description = "排程寄信任務的收信者,及信件內容")
public class ScheduleEmailRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("schedule_email_record_id")
    private Long scheduleEmailRecordId;
    
    @Schema(description = "排程信件任務ID")
    @TableField("schedule_email_task_id")
    private Long scheduleEmailTaskId;

    @Schema(description = "收信者類別,有member會員、attendees與會者、paper投稿者、paperReviewer審稿委員、")
    @TableField("recipient_category")
    private String recipientCategory;

    @Schema(description = "收信者的E-Mail")
    @TableField("email")
    private String email;

    @Schema(description = "merge tag轉換後，個人化HTML 信件內容")
    @TableField("html_content")
    private String htmlContent;

    @Schema(description = "merge tag轉換後，當HTML 信件不支援時的 純文字內容")
    @TableField("plain_text")
    private String plainText;
    
    @Schema(description = "用來儲存排程任務信件,準備寄送的附件路徑,以逗號分格的字串")
    @TableField("attachments_path")
    private String attachmentsPath;

    @Schema(description = "任務狀態 , 0為pending、1為execute 、2為finished、3為failed")
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
