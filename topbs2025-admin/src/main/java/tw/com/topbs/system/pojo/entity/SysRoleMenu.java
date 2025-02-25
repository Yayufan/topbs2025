package tw.com.topbs.system.pojo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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

	@Schema(description = "主鍵ID")
	@TableId(value = "sys_role_menu_id")
	private Long sysRoleMenuId;

	@Schema(description = "角色ID")
	@TableField("sys_role_id")
	private Long sysRoleId;

	@Schema(description = "菜單ID")
	@TableField("sys_menu_id")
	private Long sysMenuId;

	@Schema(description = "預設為0 啟用, 設置為1 禁用")
	@TableField("status")
	private Integer status;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	private LocalDateTime createDate;

	@Schema(description = "更新者")
	@TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

	@Schema(description = "更新時間")
	@TableField(value = "update_date", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateDate;
}
