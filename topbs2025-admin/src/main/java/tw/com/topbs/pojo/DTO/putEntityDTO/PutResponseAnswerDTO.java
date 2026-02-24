package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutResponseAnswerDTO {
	
	@Schema(description = "主鍵ID (如果是更新舊有答案則必傳，補填則為空)")
	private Long responseAnswerId;
	
	@NotNull
	@Schema(description = "題目ID")
	private Long formFieldId;

	@Schema(description = "回覆值")
	@NotBlank
	private String answerValue;

}
