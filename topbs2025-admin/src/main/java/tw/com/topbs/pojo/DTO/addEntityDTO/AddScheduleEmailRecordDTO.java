package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddScheduleEmailRecordDTO {

	@Schema(description = "收信者類別,有member會員、attendees與會者、paper投稿者、paperReviewer審稿委員、")
	private String recipientCategory;

	@Schema(description = "收信者的E-Mail")
	private String email;

	@Schema(description = "merge tag轉換後，個人化HTML 信件內容")
	private String htmlContent;

	@Schema(description = "merge tag轉換後，當HTML 信件不支援時的 純文字內容")
	private String plainText;

	@Schema(description = "任務狀態 , 0為pending、1為execute 、2為finished、3為failed")
	private Integer status;

}
