package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class MemberLoginInfo {

	@Schema(description = "主要信箱")
	private String email;

	@Schema(description = "密碼")
	private String password;
	
	
}
