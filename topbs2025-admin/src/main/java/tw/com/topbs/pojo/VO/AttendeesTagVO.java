package tw.com.topbs.pojo.VO;

import java.time.LocalDateTime;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;

@Data
public class AttendeesTagVO {

	@Schema(description = "主鍵ID")
	private Long attendeesId;

	@Schema(description = "0為未簽到，1為已簽到，2為已簽退")
	private Integer lastCheckinStatus;

	@Schema(description = "最後簽到/退時間")
	private LocalDateTime lastCheckinTime;

	@Schema(description = "會員資訊")
	private Member member;

	@Schema(description = "持有的標籤")
	private Set<Tag> tagSet;

}
