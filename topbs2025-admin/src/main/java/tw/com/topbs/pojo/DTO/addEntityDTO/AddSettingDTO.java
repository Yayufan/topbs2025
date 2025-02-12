package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddSettingDTO {

	@Schema(description = "早鳥優惠_一階段截止時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime earlyBirdDiscountPhaseOneDeadline;
	
	@Schema(description = "早鳥優惠_二階段截止時間 (備用)")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime earlyBirdDiscountPhaseTwoDeadline;
	
	@Schema(description = "早鳥優惠_三階段截止時間 (備用)")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime earlyBirdDiscountPhaseThreeDeadline;
	
	@Schema(description = "摘要開放投稿時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime abstractSubmissionStartTime;
	
	@Schema(description = "摘要投稿截止時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime abstractSubmissionEndTime;
	
	@Schema(description = "Slide 開放上傳時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime slideUploadStartTime;
	
	@Schema(description = "Slide 上傳截止時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime slideUploadEndTime;
	
	@Schema(description = "最後下訂單 (訂房 or City Tour ) 時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastOrderTime;
	
	@Schema(description = "最後註冊時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime lastRegistrationTime;
	
}
