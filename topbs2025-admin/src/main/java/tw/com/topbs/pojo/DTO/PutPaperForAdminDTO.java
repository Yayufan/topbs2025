package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutPaperForAdminDTO {

	@NotNull
	@Schema(description = "稿件ID")
	private Long paperId;

	@Schema(description = "稿件狀態,預設為0未審核,1為已入選,2為未入選")
	private Integer status;

	@Schema(description = "發表編號")
	private String publicationNumber;

	@Schema(description = "發表組別")
	private String publicationGroup;

	@Schema(description = "報告地點")
	private String reportLocation;

	@Schema(description = "報告時間")
	private String reportTime;

}
