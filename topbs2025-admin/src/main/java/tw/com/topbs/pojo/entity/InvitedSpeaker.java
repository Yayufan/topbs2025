package tw.com.topbs.pojo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 受邀請的講者，可能是講者，可能是座長
 * </p>
 *
 * @author Joey
 * @since 2025-04-23
 */
@ToString
@Getter
@Setter
@TableName(value = "invited_speaker", autoResultMap = true)
@Schema(name = "InvitedSpeaker", description = "受邀請的講者，可能是講者，可能是座長")
public class InvitedSpeaker implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("invited_speaker_id")
	private Long invitedSpeakerId;

	@Schema(description = "會員ID")
	@TableField("member_id")
	private Long memberId;

	@Schema(description = "講者的全名")
	@TableField("name")
	private String name;

	@Schema(description = "講者的國籍")
	@TableField("country")
	private String country;

	@Schema(description = "講者現任職的單位")
	@TableField("affiliation")
	private String affiliation;

	@Schema(description = "speaker 照片 URL")
	@TableField("photo_url")
	private String photoUrl;

	@Schema(description = "教育背景/學歷，用Json array處理")
	@TableField(value = "educational_background", typeHandler = JacksonTypeHandler.class)
	private List<String> educationalBackground;

	@Schema(description = "工作經歷")
	@TableField(value = "work_experience", typeHandler = JacksonTypeHandler.class)
	private List<String> workExperience;

	@Schema(description = "發表過的期刊/書籍/作品/研究")
	@TableField(value = "publication", typeHandler = JacksonTypeHandler.class)
	private List<String> publication;

	@Schema(description = "得到過的，榮譽或獎項")
	@TableField(value = "award", typeHandler = JacksonTypeHandler.class)
	private List<String> award;

	@Schema(description = "發佈狀態 ; 0為草稿(待發佈),1為已發佈")
	@TableField("is_published")
	private Integer isPublished;
	
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
