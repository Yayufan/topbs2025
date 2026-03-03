package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddFormResponseDTO {

	@Schema(description = "表單ID")
	@NotNull
	private Long formId;

	@Schema(description = "會員ID , 不是必填 , require_login 為 1 時會有值")
	private Long memberId;

	@Schema(description = "回覆值列表")
	@NotNull
	@Valid
	private List<AddResponseAnswerDTO> responseAnswerList;

}
