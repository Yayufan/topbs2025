package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 投稿-審稿委員 關聯表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("paper_and_paper_reviewer")
@Schema(name = "PaperAndPaperReviewer", description = "投稿-審稿委員 關聯表")
public class PaperAndPaperReviewer implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId
	private Long paperAndPaperReviewerId;
	
	
	@Schema(description = "paper_reviewer 表ID")
	@TableField("paper_reviewer_id")
	private Long paperReviewerId;

	@Schema(description = "paper 表ID")
	@TableField("paper_id")
	private Long paperId;

	@Schema(description = "評審聯繫信箱,多個信箱可用；號分隔")
	@TableField("reviewer_email")
	private String reviewerEmail;

	@Schema(description = "評審姓名")
	@TableField("reviewer_name")
	private String reviewerName;

	@Schema(description = "評審對投稿所評分數")
	@TableField("score")
	private Integer score;

	@Schema(description = "啟用狀態,0為啟用,1為禁用")
	@TableField("status")
	private Integer status;

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
}
