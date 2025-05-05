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
 * 菜單表-最底層細部權限也存在這張表,包含路由、路由組件、路由參數... 組裝動態路由返回給前端
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Getter
@Setter
@TableName("sys_menu")
@Schema(name = "SysMenu", description = "菜單表-最底層細部權限也存在這張表,包含路由、路由組件、路由參數... 組裝動態路由返回給前端")
public class SysMenu implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "菜單ID")
	@TableId("sys_menu_id")
	private Long sysMenuId;

	@Schema(description = "父級菜單ID")
	@TableField("parent_id")
	private Long parentId;

	@Schema(description = "菜單名稱")
	@TableField("menu_name")
	private String menuName;

	@Schema(description = "菜單類型( 主菜單M  功能表C  按鈕功能F ) ,  主菜單M,只代表他為一個資料夾Menu ; 功能表C,代表有實際功能的核心頁面Core  ; 功能按鈕F ,為控制底層權限增刪改查function")
	@TableField("menu_type")
	private String menuType;

	@Schema(description = "前端路由地址")
	@TableField("path")
	private String path;

	@Schema(description = "前端路由參數")
	@TableField("query_params")
	private String queryParams;

	@Schema(description = "前端路由組件")
	@TableField("component")
	private String component;

	@Schema(description = "是否顯示, 0為顯示, 1為隱藏")
	@TableField("visible")
	private Integer visible;

	@Schema(description = "菜單圖標")
	@TableField("icon")
	private String icon;

	@Schema(description = "底層權限標示符")
	@TableField("permission")
	private String permission;

	@Schema(description = "是否為緩存路由 , 0為是 , 1為否")
	@TableField("is_cache")
	private Integer isCache;

	@Schema(description = "是否為外連結 0為是 , 1為否")
	@TableField("is_frame")
	private Integer isFrame;

	@Schema(description = "顯示順序")
	@TableField("order_num")
	private Integer orderNum;

	@Schema(description = "備註, 通常備註這個菜單及權限作用")
	@TableField("remark")
	private String remark;

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

	@Schema(description = "邏輯刪除 預設為0 代表存在 , 設置為1 刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;

}
