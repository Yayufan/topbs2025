package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddResponseAnswerDTO {

	@Schema(description = "表單欄位ID")
	@NotNull
    private Long formFieldId;

    @Schema(description = "回覆值")
    @NotBlank
    private String answerValue;
	
}
