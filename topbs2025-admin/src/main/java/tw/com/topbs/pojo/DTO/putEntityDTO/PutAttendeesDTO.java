package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutAttendeesDTO {

	@Schema(description = "主鍵ID")
	private Long attendeesId;

	@Schema(description = "會員ID")
	private Long memberId;

	@Schema(description = "與會者mail ， 新增時從會員拿到")
	private String email;
}
