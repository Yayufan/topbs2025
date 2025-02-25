package tw.com.topbs.system.pojo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 角色表 - 透過設置角色達成較廣泛的權限管理
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Getter
@Setter
@TableName("sys_role")
@Schema(name = "SysRole", description = "角色表 - 透過設置角色達成較廣泛的權限管理")
public class SysRole implements Serializable {

	private static final long serialVersionUID = 1L;

	@TableId("sys_role_id")
	private Long sysRoleId;

	@Schema(description = "角色名稱")
	@TableField("role_name")
	private String roleName;

	@Schema(description = "角色權限標示符,假設是admin, SaToken 會判斷是否具有Admin的角色")
	@TableField("role_key")
	private String roleKey;

	@Schema(description = "資料範圍 ( 1為全部範圍 , 2為自定義範圍 )")
	@TableField("data_scope")
	private Integer dataScope;

	@Schema(description = "菜單關聯是否關聯開啟, 預設為1 聯動開啟 , 簡單來說就是,具有父菜單 , 子菜單是否開啟")
	@TableField("menu_check_stricktly")
	private Integer menuCheckStricktly;

	@Schema(description = "角色顯示優先級順序")
	@TableField("role_sort")
	private Integer roleSort;

	@Schema(description = "角色狀態 預設為0 啟用 ,  設置為1禁用")
	@TableField("status")
	private Integer status;

	@Schema(description = "邏輯刪除 預設為0 代表存在 , 設置為1 刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;

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

	@Schema(description = "備註, 通常用來寫這個角色要做些什麼事情, 來凸顯為什麼需要某些權限")
	@TableField("remark")
	private String remark;
}
