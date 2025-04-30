package tw.com.topbs.pojo.entity;

import java.io.Serializable;
import java.time.LocalDate;
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

/**
 * <p>
 * 投稿資料表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("paper")
@Schema(name = "Paper", description = "投稿資料表")
public class Paper implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("paper_id")
	private Long paperId;

	@Schema(description = "會員ID")
	@TableField("member_id")
	private Long memberId;

	@Schema(description = "報告方式")
	@TableField("presentation_type")
	private String presentationType;

	@Schema(description = "投稿類別")
	@TableField("abs_type")
	private String absType;

	@Schema(description = "文章屬性")
	@TableField("abs_prop")
	private String absProp;

	@Schema(description = "稿件主題_國際會議所以只收英文")
	@TableField("abs_title")
	private String absTitle;

	@Schema(description = "第一作者")
	@TableField("first_author")
	private String firstAuthor;
	
	@Schema(description = "第一作者生日，用來判斷是否符合獎項資格")
	@TableField("first_author_birthday")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate firstAuthorBirthday;

	@Schema(description = "主講者")
	@TableField("speaker")
	private String speaker;

	@Schema(description = "主講者單位_國際會議所以只收英文")
	@TableField("speaker_affiliation")
	private String speakerAffiliation;

	@Schema(description = "通訊作者")
	@TableField("corresponding_author")
	private String correspondingAuthor;

	@Schema(description = "通訊作者E-Mail")
	@TableField("corresponding_author_email")
	private String correspondingAuthorEmail;

	@Schema(description = "通訊作者聯絡電話")
	@TableField("corresponding_author_phone")
	private String correspondingAuthorPhone;

	@Schema(description = "全部作者")
	@TableField("all_author")
	private String allAuthor;

	@Schema(description = "全部作者單位")
	@TableField("all_author_affiliation")
	private String allAuthorAffiliation;

	@Schema(description = "稿件狀態,預設為0未審核,1為已入選,2為未入選")
	@TableField("status")
	private Integer status;

	@Schema(description = "發表編號")
	@TableField("publication_number")
	private String publicationNumber;

	@Schema(description = "發表組別")
	@TableField("publication_group")
	private String publicationGroup;

	@Schema(description = "報告地點")
	@TableField("report_location")
	private String reportLocation;

	@Schema(description = "報告時間")
	@TableField("report_time")
	private String reportTime;

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
