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
 * 會員表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("member")
@Schema(name = "Member", description = "會員表")
public class Member implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("member_id")
	private Long memberId;

	@Schema(description = "E-Mail")
	@TableField("email")
	private String email;

	@Schema(description = "密碼")
	@TableField("password")
	private String password;

	@Schema(description = "頭銜 - 前墜詞")
	@TableField("title")
	private String title;

	@Schema(description = "名字, 華人的名在後  , 外國人的名在前")
	@TableField("first_name")
	private String firstName;

	@Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	@TableField("last_name")
	private String lastName;

	@Schema(description = "國家")
	@TableField("country")
	private String country;
	
	@Schema(description = "匯款帳號-後五碼  台灣會員使用")
	@TableField("remit_account_last5")
	private String remitAccountLast5;

	@Schema(description = "單位(所屬的機構)")
	@TableField("affiliation")
	private String affiliation;

	@Schema(description = "職稱")
	@TableField("job_title")
	private String jobTitle;

	@Schema(description = "電話號碼,這邊要使用 國碼-號碼")
	@TableField("phone")
	private String phone;

	@Schema(description = "用於分類會員資格, 1為 Invited Speaker 、 2為 Board Member 、 3為 Normal Member 、 4為 Companion")
	@TableField("category")
	private Integer category;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField(value = "update_by", fill = FieldFill.UPDATE)
	private String updateBy;

	@Schema(description = "最後修改時間")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;
}
