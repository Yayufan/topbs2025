package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutFormResponseDTO {

	@Schema(description = "表單ID")
	@NotNull
	private Long formId;

	@Schema(description = "回覆ID")
	@NotNull
	private Long formResponseId;
	
	@Schema(description = "回覆值列表")
	@NotNull
	@Valid
	private List<PutResponseAnswerDTO> responseAnswerList;

}
