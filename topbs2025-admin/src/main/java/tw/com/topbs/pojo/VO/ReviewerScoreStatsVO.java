package tw.com.topbs.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReviewerScoreStatsVO {

	@Schema(description = "paper_reviewer 表ID")
	private Long paperReviewerId;

	@Schema(description = "評審聯繫信箱,多個信箱可用；號分隔")
	private String reviewerEmail;

	@Schema(description = "評審姓名")
	private String reviewerName;

	@Schema(description = "一階段審核為:first_review，二階段審核為:second_review，三階段(可不用)")
	private String reviewStage;

	@Schema(description = "評審 應 審核稿件數量")
	private int totalReviewCount;

	@Schema(description = "評審 已 審核稿件數量")
	private int completedReviewCount;
}
