package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemberLoginInfo {

	@Schema(description = "主要信箱")
	private String email;

	@Schema(description = "密碼")
	private String password;
	
	@NotBlank
	@Schema(description = "驗證碼key")
	private String verificationKey;
	
	@NotBlank
	@Schema(description = "用戶輸入的驗證碼")
	private String verificationCode;
	
	
}
