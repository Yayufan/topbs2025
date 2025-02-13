package tw.com.topbs.pojo.DTO.putEntityDTO;

import com.baomidou.mybatisplus.annotation.TableField;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutMemberDTO {

	@NotNull
	@Schema(description = "主鍵ID")
	private Long memberId;

	@NotBlank
	@Schema(description = "頭銜 - 前墜詞")
	@TableField("title")
	private String title;

	@NotBlank
	@Schema(description = "名字, 華人的名在後  , 外國人的名在前")
	@TableField("first_name")
	private String firstName;

	@NotBlank
	@Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	@TableField("last_name")
	private String lastName;

	@NotBlank
	@Schema(description = "國家")
	private String country;

	@Schema(description = "匯款帳號-後五碼  台灣會員使用")
	private String remitAccountLast5;

	@NotNull
	@Schema(description = "用於分類會員資格, 1為 Invited Speaker 、 2為 Board Member 、 3為 Normal Member 、 4為 Companion")
	private Integer category;

	@NotBlank
	@Schema(description = "單位(所屬的機構)")
	private String affiliation;

	@NotBlank
	@Schema(description = "職稱")
	private String jobTitle;

	@NotBlank
	@Schema(description = "電話號碼,這邊要使用 國碼-號碼")
	private String phone;

}
