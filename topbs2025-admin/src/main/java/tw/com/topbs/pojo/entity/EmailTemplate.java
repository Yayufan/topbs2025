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
 * 信件模板表
 * </p>
 *
 * @author Joey
 * @since 2025-01-16
 */
@Getter
@Setter
@TableName("email_template")
@Schema(name = "EmailTemplate", description = "信件模板表")
public class EmailTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("email_template_id")
    private Long emailTemplateId;

    @Schema(description = "類別，用於拆分Email模板適合的人群")
    @TableField("category")
    private String category;
    
    @Schema(description = "信件模板名稱")
    @TableField("name")
    private String name;
    
    @Schema(description = "信件模板描述")
    @TableField("description")
    private String description;

    @Schema(description = "用於儲存unlayer 的 design JSON數據")
    @TableField("design")
    private String design;

    @Schema(description = "HTML 信件內容")
    @TableField("html_content")
    private String htmlContent;

    @Schema(description = "當HTML 信件不支援時的 純文字內容")
    @TableField("plain_text")
    private String plainText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "創建時間")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "創建者")
    @TableField(value="create_by", fill = FieldFill.INSERT)
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最後更新時間")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "最後更新者")
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    @Schema(description = "邏輯刪除(0為存在,1為刪除)")
    @TableField("is_deleted")
    @TableLogic
    private String isDeleted;
}
