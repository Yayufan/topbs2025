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
 * 角色與菜單 - 多對多關聯表
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Getter
@Setter
@TableName("sys_role_menu")
@Schema(name = "SysRoleMenu", description = "角色與菜單 - 多對多關聯表")
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色ID")
    @TableId(value =  "role_id", type = IdType.INPUT)
    private Long roleId;

    @Schema(description = "菜單ID")
    @TableField("menu_id")
    private Long menuId;
}
