package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 往年與會者名單
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Getter
@Setter
@TableName("attendees_history")
@Schema(name = "AttendeesHistory", description = "往年與會者名單")
public class AttendeesHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("attendees_history_id")
    private Long attendeesHistoryId;

    @Schema(description = "參與時的年份")
    @TableField("year")
    private Integer year;

    @Schema(description = "身分證字號 OR 護照號碼, 用於當作比對的第一標準")
    @TableField("id_card")
    private String idCard;

    @Schema(description = "與會者的Email,用於用於當作比對的第二標準")
    @TableField("email")
    private String email;

    @Schema(description = "姓名,不參與比對,只是方便辨識誰參與而已")
    @TableField("name")
    private String name;

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
    private Integer isDeleted;
}
