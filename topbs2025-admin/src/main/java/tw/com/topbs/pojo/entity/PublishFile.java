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
 * 發佈檔案表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("publish_file")
@Schema(name = "PublishFile", description = "發佈檔案表")
public class PublishFile implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "檔案表，主鍵ID")
	@TableId("publish_file_id")
	private Long publishFileId;

	@Schema(description = "群組類型，用於分別是屬於哪個頁面的檔案")
	@TableField("group_type")
	private String groupType;

	@Schema(description = "二級類別,如果群組類別底下還有細分類別,可以用這個")
	@TableField("type")
	private String type;

	@Schema(description = "檔名")
	@TableField("name")
	private String name;

	@Schema(description = "檔案描述")
	@TableField("description")
	private String description;

	@Schema(description = "儲存地址")
	@TableField("path")
	private String path;

	@Schema(description = "檔案封面縮圖URL")
	@TableField("cover_thumbnail_url")
	private String coverThumbnailUrl;

	@Schema(description = "外部鏈結")
	@TableField("link")
	private String link;

	@Schema(description = "排序值")
	@TableField("sort")
	private Integer sort;

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
