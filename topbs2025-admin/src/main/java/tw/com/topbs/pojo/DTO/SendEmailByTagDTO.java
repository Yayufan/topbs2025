package tw.com.topbs.pojo.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendEmailByTagDTO {

	@Schema(description = "複數tag作為篩選條件，沒給tag代表寄給全部會員")
	private List<Long> tagIdList;

	@Valid
	@NotNull
	private SendEmailDTO sendEmailDTO;

}
