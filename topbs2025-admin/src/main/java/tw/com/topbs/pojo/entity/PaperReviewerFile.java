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
 * 給審稿委員的公文檔案和額外]資料
 * </p>
 *
 * @author Joey
 * @since 2025-06-03
 */
@Getter
@Setter
@TableName("paper_reviewer_file")
@Schema(name = "PaperReviewerFile", description = "給審稿委員的公文檔案和額外]資料")
public class PaperReviewerFile implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("paper_reviewer_file_id")
	private Long paperReviewerFileId;

	@Schema(description = "審稿委員ID")
	@TableField("paper_reviewer_id")
	private Long paperReviewerId;

	@Schema(description = "可分類成公文檔案、審稿規範之類的，大部分是official_document")
	@TableField("type")
	private String type;

	@Schema(description = "檔案在minio儲存的路徑")
	@TableField("path")
	private String path;

	@Schema(description = "檔案名稱-可與傳送時不同")
	@TableField("file_name")
	private String fileName;

	@Schema(description = "創建者")
	@TableField("create_by")
	private String createBy;

	@Schema(description = "創建時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField("update_by")
	private String updateBy;

	@Schema(description = "最後修改時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Byte isDeleted;
}
