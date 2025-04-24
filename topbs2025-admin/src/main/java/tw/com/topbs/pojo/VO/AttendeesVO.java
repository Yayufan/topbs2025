package tw.com.topbs.pojo.VO;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;

import io.swagger.v3.oas.annotations.media.Schema;
import tw.com.topbs.pojo.entity.Member;

public class AttendeesVO {

	@Schema(description = "主鍵ID")
	private Long attendeesId;
	
	@Schema(description = "0為未簽到，1為已簽到，2為已簽退")
	@TableField("last_checkin_status")
	private Integer lastCheckinStatus;

	@Schema(description = "最後簽到/退時間")
	@TableField("last_checkin_time")
	private LocalDateTime lastCheckinTime;
	
	@Schema(description = "會員資訊")
	private Member member;
	
}
