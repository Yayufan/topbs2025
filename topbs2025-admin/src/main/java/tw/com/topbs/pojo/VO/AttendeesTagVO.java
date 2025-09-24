package tw.com.topbs.pojo.VO;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;

@Data
public class AttendeesTagVO {

	@Schema(description = "主鍵ID")
	private Long attendeesId;
	
	@Schema(description = "現在是否已簽到；預設為false,未簽到或處於簽退狀態")
	private Boolean isCheckedIn = false;
	
	@Schema(description = "參與者流水序號")
	private Integer sequenceNo;

	@Schema(description = "會員資訊")
	private Member member;

	@Schema(description = "持有的標籤")
	private List<Tag> tagList;

	@Schema(description = "簽到記錄")
	private List<CheckinRecord> checkinRecordList;

}
