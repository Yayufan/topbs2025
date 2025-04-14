package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendEmailDTO {

	@Schema(description = "信件主旨")
	@NotBlank
	private String subject;

	@Schema(description = "HTML 信件內容")
	@NotBlank
	private String htmlContent;

	@Schema(description = "當HTML 信件不支援時的 純文字內容")
	@NotBlank
	private String plainText;

	@Schema(description = "當HTML 信件不支援時的 純文字內容")
	@NotNull
	private Boolean isTest;

	@Schema(description = "當勾選測試信件時，要接收測試信件的 信箱")
	private String testEmail;

}
