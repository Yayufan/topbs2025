package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddPaperReviewerToPaperDTO {

	@NotBlank
	@Schema(description = "審核階段,first_review 或 second_review 或 third_review ")
	private String reviewStage ;
	
	// 確保 `targetPaperReviewerIdList` 不能是 `null`
	// 不使用 @Size(min = 1)，這樣允許空列表 `[]`
	@Schema(description = "稿件評審  ID 列表")
	@NotNull(message = "稿件評審 ID 清單不能為null")
	private List<Long> targetPaperReviewerIdList;

	@Schema(description = "稿件 ID")
	@NotNull(message = "稿件 ID 不能為null")
	private Long paperId;

}
