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
 * 與會者 與 標籤 的關聯表
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Getter
@Setter
@TableName("attendees_tag")
@Schema(name = "AttendeesTag", description = "與會者 與 標籤 的關聯表")
public class AttendeesTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主鍵ID")
    @TableId("attendees_tag_id")
    private Long attendeesTagId;

    @Schema(description = "與會者ID")
    @TableField("attendees_id")
    private Long attendeesId;

    @Schema(description = "標籤ID")
    @TableField("tag_id")
    private Long tagId;
}
