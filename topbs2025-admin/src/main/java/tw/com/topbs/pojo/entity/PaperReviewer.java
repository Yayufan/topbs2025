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
 * 稿件評審資料表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("paper_reviewer")
@Schema(name = "PaperReviewer", description = "稿件評審資料表")
public class PaperReviewer implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("paper_reviewer_id")
	private Long paperReviewerId;

	@Schema(description = "評審類別,可用，號分隔, 表示可以審多個領域的Paper")
	@TableField("abs_type_list")
	private String absTypeList;

	@Schema(description = "評審聯繫信箱,多個信箱可用；號分隔")
	@TableField("email")
	private String email;

	@Schema(description = "評審姓名")
	@TableField("name")
	private String name;

	@Schema(description = "評審電話")
	@TableField("phone")
	private String phone;

	@Schema(description = "評審帳號")
	@TableField("account")
	private String account;

	@Schema(description = "評審密碼")
	@TableField("password")
	private String password;

	@Schema(description = "評審公文檔案01")
	@TableField("official_document_file_url01")
	private String officialDocumentFileUrl01;

	@Schema(description = "評審公文檔案02")
	@TableField("official_document_file_url02")
	private String officialDocumentFileUrl02;

	@Schema(description = "評審公文檔案03")
	@TableField("official_document_file_url03")
	private String officialDocumentFileUrl03;

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
