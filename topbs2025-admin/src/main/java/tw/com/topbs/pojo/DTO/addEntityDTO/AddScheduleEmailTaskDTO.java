package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddScheduleEmailTaskDTO {

	@Schema(description = "收信者類別,有member會員、attendees與會者、paper投稿者、paperReviewer審稿委員、")
	private String recipientCategory;

	@Schema(description = "此排程信件任務的描述,用於補充")
	private String description;

	@Schema(description = "任務開始時間")
	private LocalDateTime startTime;

	@Schema(description = "信件主旨")
	private String subject;

	@Schema(description = "排程信件使用模板，用於儲存unlayer 的 design JSON數據")
	private String design;

	@Schema(description = "排程信件使用模板，HTML 信件內容")
	private String htmlContent;

	@Schema(description = "排程信件使用模板，當HTML 信件不支援時的 純文字內容")
	private String plainText;

	@Schema(description = "預期消耗的電子郵件數量")
	private Integer expectedEmailVolume;

}
