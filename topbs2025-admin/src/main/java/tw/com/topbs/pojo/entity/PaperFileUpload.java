package tw.com.topbs.pojo.entity;

import java.io.Serializable;
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
 * 投稿檔案資料表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("paper_file_upload")
@Schema(name = "PaperFileUpload", description = "投稿檔案資料表")
public class PaperFileUpload implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("paper_file_upload_id")
	private Long paperFileUploadId;

	@Schema(description = "投稿ID")
	@TableField("paper_id")
	private Long paperId;

	@Schema(description = "分類成 abstructs_pdf, abstructs_docx, slide, offical_document; 用來接收 投稿PDF、投稿WORD、投稿PPT、公文檔案")
	@TableField("type")
	private String type;

	@Schema(description = "檔案名稱-可與傳送時不同")
	@TableField("file_name")
	private String fileName;

	@Schema(description = "檔案在minio儲存的路徑")
	@TableField("path")
	private String path;

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
