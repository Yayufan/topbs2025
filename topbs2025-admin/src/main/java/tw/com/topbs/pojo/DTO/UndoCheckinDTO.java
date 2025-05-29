package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UndoCheckinDTO {

	@NotNull
	@Schema(description = "與會者ID")
	private Long attendeesId;
}
