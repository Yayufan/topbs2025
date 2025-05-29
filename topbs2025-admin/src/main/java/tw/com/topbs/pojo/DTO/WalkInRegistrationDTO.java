package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalkInRegistrationDTO {
	
	@Schema(description = "中文姓名，外國人非必填，台灣人必填")
	private String chineseName;
	
	@Schema(description = "英文-名字, 華人的名在後  , 外國人的名在前")
	private String firstName;

	@Schema(description = "英文-姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	private String lastName;
	
	@NotBlank
	@Schema(description = "E-Mail")
	private String email;
	
	@NotNull
	@Schema(description = "用於分類會員資格, 1為 Member，2為 Others，3為 Non-Member，4為 MVP，5為 Speaker，6為 Moderator，7為 Staff")
	private Integer category;
	
	
	
}
