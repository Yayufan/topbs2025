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
 * 標籤表,用於對Member進行分組
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Getter
@Setter
@TableName("tag")
@Schema(name = "Tag", description = "標籤表,用於對Member進行分組")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "唯一主鍵")
    @TableId("tag_id")
    private Long tagId;

    @Schema(description = "tag的分類,用於之後擴充member表有對應的標籤可用, 擴增表也有對應的標籤可用")
    @TableField("type")
    private String type;

    @Schema(description = "標籤名稱,用於顯示")
    @TableField("name")
    private String name;

    @Schema(description = "標籤的描述")
    @TableField("description")
    private String description;

    @Schema(description = "標籤的狀態, 0為啟用  1為禁用")
    @TableField("status")
    private Integer status;

    @Schema(description = "標籤的顯示顏色")
    @TableField("color")
    private String color;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "創建時間")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "創建者")
    @TableField("create_by")
    private String createBy;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最後更新時間")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "最後更新者")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "邏輯刪除(0為存在,1為刪除)")
    @TableField("is_deleted")
    @TableLogic
    private String isDeleted;
}
