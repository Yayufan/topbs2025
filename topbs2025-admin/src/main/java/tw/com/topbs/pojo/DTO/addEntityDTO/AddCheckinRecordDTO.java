package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddCheckinRecordDTO {

	@Schema(description = "與會者ID")
	private Long attendeesId;

	@Schema(description = "簽到/退地點,保留欄位，未來擴展")
	private String location;

	@Schema(description = "動作類型, 1=簽到, 2=簽退")
	private Integer actionType;

}
