package tw.com.topbs.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AssignedReviewersVO {

	@Schema(description = "關聯表主鍵ID")
	private Long paperAndPaperReviewerId;
	
	@Schema(description = "paper 表ID")
	private Long paperId;
	
	@Schema(description = "審稿委員ID")
	private Long paperReviewerId;
	
	@Schema(description = "評審姓名")
	private String reviewerName;
	
	@Schema(description = "一階段審核為:first_review，二階段審核為:second_reviewer，三階段(可不用)")
	private String reviewStage;
	
	
	
}
