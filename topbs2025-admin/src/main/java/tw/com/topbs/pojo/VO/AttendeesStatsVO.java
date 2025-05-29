package tw.com.topbs.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AttendeesStatsVO {

	@Schema(description = "應到人數")
	private Integer totalShouldAttend;

	@Schema(description = "實到人數")
	private Integer totalCheckedIn;

	@Schema(description = "未到人數")
	private Integer totalNotArrived;

	@Schema(description = "已在場人數（簽到但尚未簽退）")
	private Integer totalOnSite;

	@Schema(description = "已離場人數（簽到並簽退）")
	private Integer totalLeft;
}
