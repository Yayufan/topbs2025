package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 文章表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("article")
@Schema(name = "Article", description = "文章表")
public class Article implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "文章ID")
	@TableId("article_id")
	private Long articleId;

	@Schema(description = "類別ID")
	@TableField("category_id")
	private Long categoryId;

	@Schema(description = "同步ID")
	@TableField("async_id")
	private Long asyncId;

	@Schema(description = "文章標題")
	@TableField("title")
	private String title;

	@Schema(description = "對文章的描述")
	@TableField("description")
	private String description;

	@Schema(description = "群組 - 用於分類文章的群組")
	@TableField("group_type")
	private String groupType;

	@Schema(description = "文章的類型")
	@TableField("type")
	private String type;

	@Schema(description = "封面縮圖URL")
	@TableField("cover_thumbnail_url")
	private String coverThumbnailUrl;

	@Schema(description = "公告此文章的日期")
	@TableField("announcement_date")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate announcementDate;

	@Schema(description = "HTML文章內容")
	@TableField("content")
	private String content;

	@Schema(description = "瀏覽數")
	@TableField("views")
	private Integer views;

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
