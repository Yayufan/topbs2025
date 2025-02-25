package tw.com.topbs.pojo.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgetPwdDTO {
	@Email(message = "必須是形式完整的電子郵件位址")
	@NotBlank(message = "電子郵件不能為空")
	private String email;
}
