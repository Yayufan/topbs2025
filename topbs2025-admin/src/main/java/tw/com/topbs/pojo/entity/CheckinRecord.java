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
 * 簽到退紀錄
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Getter
@Setter
@TableName("checkin_record")
@Schema(name = "CheckinRecord", description = "簽到退紀錄")
public class CheckinRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("checkin_record_id")
    private Long checkinRecordId;

    @Schema(description = "與會者ID")
    @TableField("attendees_id")
    private Long attendeesId;

    @Schema(description = "簽到/退地點,保留欄位，未來擴展")
    @TableField("location")
    private String location;

    @Schema(description = "動作類型, 1=簽到, 2=簽退")
    @TableField("action_type")
    private Byte actionType;

    @Schema(description = "簽到/退時間")
    @TableField("action_time")
    private LocalDateTime actionTime;

    @Schema(description = "備註")
    @TableField("remark")
    private String remark;

    @Schema(description = "創建者")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "創建時間")
    @TableField(value = "create_date", fill = FieldFill.INSERT)
    private LocalDateTime createDate;

    @Schema(description = "最後修改者")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "最後修改時間")
    @TableField(value = "update_date", fill = FieldFill.UPDATE)
    private LocalDateTime updateDate;

    @Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
