package tw.com.topbs.pojo.VO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CheckinRecordVO {

	@Schema(description = "主鍵ID")
	private Long checkinRecordId;

	@Schema(description = "與會者VO對象(含基本資料)")
	private AttendeesVO attendeesVO;

	@Schema(description = "簽到/退地點,保留欄位，未來擴展")
	private String location;

	@Schema(description = "動作類型, 1=簽到, 2=簽退")
	private Integer actionType;

	@Schema(description = "簽到/退時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime actionTime;

	@Schema(description = "備註")
	private String remark;

}
