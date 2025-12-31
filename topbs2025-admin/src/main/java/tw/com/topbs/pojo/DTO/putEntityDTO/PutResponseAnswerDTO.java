package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutResponseAnswerDTO {

	@Schema(description = "主鍵ID")
	@NotNull
	private Long responseAnswerId;

	@Schema(description = "回覆值")
	@NotBlank
	private String answerValue;

}
