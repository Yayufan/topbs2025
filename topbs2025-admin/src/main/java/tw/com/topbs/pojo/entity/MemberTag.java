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
 * member表 和 tag表的關聯表
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Getter
@Setter
@TableName("member_tag")
@Schema(name = "MemberTag", description = "member表 和 tag表的關聯表")
public class MemberTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "關聯表唯一主鍵")
    @TableId("id")
    private Long id;

    @Schema(description = "會員ID")
    @TableField("member_id")
    private Long memberId;

    @Schema(description = "標籤ID")
    @TableField("tag_id")
    private Long tagId;
}
