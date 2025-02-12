package tw.com.topbs.system.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用戶與角色 - 多對多關聯表
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Getter
@Setter
@TableName("sys_user_role")
@Schema(name = "SysUserRole", description = "用戶與角色 - 多對多關聯表")
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用戶ID")
    @TableId(value = "user_id" , type = IdType.INPUT)
    private Long userId;

    @Schema(description = "角色ID")
    @TableField("role_id")
    private Long roleId;
}
