package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutMemberIdDTO {

	@NotNull
	@Schema(description = "主鍵ID")
	private Long memberId;
	
}
