package tw.com.topbs.pojo.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgetPwdDTO {
	
	@Email(message = "must be an E-Mail address format")
	@NotBlank(message = "E-Mail can not be blank")
	private String email;
}
