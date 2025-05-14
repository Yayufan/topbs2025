package tw.com.topbs.pojo.DTO.putEntityDTO;

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
	@Schema(description = "同時作為護照號碼 和 台灣身分證字號使用")
	private String idCard;

	@Schema(description = "群組代號, 用UUID randomUUID() 產生")
	private String group;

	@Schema(description = "當如果今天member具有群組, 那麼用這個確認他是主報名者 master,還是子報名者 slave , 這也是讓子報名者更換成主報名者的機制")
	private String groupRole;

	@NotBlank
	@Schema(description = "頭銜 - 前墜詞")
	private String title;

	@Schema(description = "中文姓名，外國人非必填，台灣人必填")
	private String chineseName;

	@NotBlank
	@Schema(description = "名字, 華人的名在後  , 外國人的名在前")
	private String firstName;

	@NotBlank
	@Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	private String lastName;

	@NotBlank
	@Schema(description = "國家")
	private String country;

	@Schema(description = "匯款帳號-後五碼  台灣會員使用")
	private String remitAccountLast5;

	@NotNull
	@Schema(description = "用於分類會員資格, 1為 Member，2為 Others，3為 Non-Member，4為 MVP，5為 Speaker，6為 Moderator，7為 Staff")
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
