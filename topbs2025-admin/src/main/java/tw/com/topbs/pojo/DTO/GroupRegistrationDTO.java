package tw.com.topbs.pojo.DTO;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GroupRegistrationDTO {

	@NotNull
	@Size(min = 5, message = "The number of people must be greater than or equal to 5")
	private List<@Valid AddGroupMemberDTO> groupMembers;

	@NotBlank
	private String verificationCode;

	@NotBlank
	private String verificationKey;
}
