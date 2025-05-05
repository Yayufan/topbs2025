package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * paper表 和 tag表的關聯表
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Getter
@Setter
@TableName("paper_tag")
@Schema(name = "PaperTag", description = "paper表 和 tag表 的關聯表")
public class PaperTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "關聯表唯一主鍵")
    @TableId("id")
    private Long id;

    @Schema(description = "稿件ID")
    @TableField("paper_id")
    private Long paperId;

    @Schema(description = "標籤ID")
    @TableField("tag_id")
    private Long tagId;
}
