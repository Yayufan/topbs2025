package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddGroupMemberDTO {
	
	@NotBlank
	@Schema(description = "頭銜 - 前墜詞")
	private String title;

	@NotBlank
	@Schema(description = "名字, 華人的名在後  , 外國人的名在前")
	private String firstName;

	@NotBlank
	@Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	private String lastName;

	@NotBlank
	@Schema(description = "E-Mail")
	private String email;

	@NotBlank
	@Schema(description = "密碼")
	private String password;

	@NotBlank
	@Schema(description = "國家")
	private String country;
	
	@Schema(description = "匯款帳號-後五碼  台灣會員使用")
	private String remitAccountLast5;

	@NotNull
	@Schema(description = "用於分類會員資格, 1為 Non-member、 2為Member 、 3為Others")
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
