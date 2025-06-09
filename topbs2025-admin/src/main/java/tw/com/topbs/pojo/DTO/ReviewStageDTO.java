package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewStageDTO {

	@Schema(description = "審核階段,first_review 或 second_review")
	@NotBlank
	private String reviewStage;
}
