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
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
@Getter
@Setter
@TableName("attendees")
@Schema(name = "Attendees", description = "參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用")
public class Attendees implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("attendees_id")
	private Long attendeesId;

	@Schema(description = "會員ID")
	@TableField("member_id")
	private Long memberId;

	@Schema(description = "0為未簽到，1為已簽到，2為已簽退")
	@TableField("last_checkin_status")
	private Integer lastCheckinStatus;

	@Schema(description = "最後簽到/退時間")
	@TableField("last_checkin_time")
	private LocalDateTime lastCheckinTime;

	@Schema(description = "與會者mail ， 新增時從會員拿到")
	@TableField("email")
	private String email;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField(value = "update_by", fill = FieldFill.UPDATE)
	private String updateBy;

	@Schema(description = "最後修改時間")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;
}
