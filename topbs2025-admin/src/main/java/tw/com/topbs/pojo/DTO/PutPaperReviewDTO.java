package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutPaperReviewDTO {
	
	
	@Schema(description = "主鍵ID")
	private Long paperAndPaperReviewerId;
	
	@NotNull
	@Schema(description = "paper_reviewer 表ID")
	private Long paperReviewerId;

	@NotNull
	@Schema(description = "paper 表ID")
	private Long paperId;
	
	@NotBlank
	@Schema(description = "一階段審核為:first_review，二階段審核為:second_reviewer，三階段(暫時用不到)")
	private String reviewStage;

	@NotNull
	@Schema(description = "評審對投稿所評分數")
	private Integer score;
}
