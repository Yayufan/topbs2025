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
 * 用戶表 - 存取系統用戶個人信息
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Getter
@Setter
@TableName("sys_user")
@Schema(name = "SysUser", description = "用戶表 - 存取系統用戶個人信息")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("user_id")
    private Long userId;

    @Schema(description = "部門ID")
    @TableField("dept_id")
    private Long deptId;

    @Schema(description = "帳號")
    @TableField("account")
    private String account;

    @Schema(description = "密碼")
    @TableField("password")
    private String password;

    @Schema(description = "真實姓名")
    @TableField("real_name")
    private String realName;

    @Schema(description = "英文姓名(暱稱)")
    @TableField("nick_name")
    private String nickName;

    @Schema(description = "信箱")
    @TableField("email")
    private String email;

    @Schema(description = "電話號碼")
    @TableField("phone")
    private String phone;

    @Schema(description = "預設為0 啟用, 設置為1 禁用")
    @TableField("status")
    private Integer status;
    
    @Schema(description = "備註,通常寫這個使用者的主要角色")
    @TableField("remark")
    private String remark;

    @Schema(description = "預設為0代表存在, 更改為1代表刪除")
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;

    @Schema(description = "創建者")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "創建時間")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新者")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "更新時間")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}
