package tw.com.topbs.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.Member;

@Data
public class AttendeesVO {

	@Schema(description = "主鍵ID")
	private Long attendeesId;
	
	@Schema(description = "主鍵ID")
	private Long memberId;
	
	@Schema(description = "參與者流水序號")
	private Integer sequenceNo;
	
	@Schema(description = "會員資訊")
	private Member member;
	
	@Schema(description = "是否為去年與會者,true為是,false為否")
	private Boolean isLastYearAttendee = false;
	
}
