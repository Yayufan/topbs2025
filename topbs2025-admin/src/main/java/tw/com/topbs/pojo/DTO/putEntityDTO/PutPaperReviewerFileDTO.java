package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutPaperReviewerFileDTO {

	@Schema(description = "主鍵ID")
	private Long paperReviewerFileId;

	@Schema(description = "審稿委員ID")
	private Long paperReviewerId;

}
