package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutCheckinRecordDTO {

	@Schema(description = "主鍵ID")
	private Long checkinRecordId;

	@Schema(description = "與會者ID")
	private Long attendeesId;

	@Schema(description = "簽到/退地點,保留欄位，未來擴展")
	private String location;

	@Schema(description = "動作類型, 1=簽到, 2=簽退")
	private Integer actionType;

	@Schema(description = "簽到/退時間")
	private LocalDateTime actionTime;

	@Schema(description = "備註")
	private String remark;

}
