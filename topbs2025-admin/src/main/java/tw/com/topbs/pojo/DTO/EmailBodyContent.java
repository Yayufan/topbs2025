package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailBodyContent {

	@Schema(description = "HTML信件內容")
	private String htmlContent;
	
	@Schema(description = "純文字信件內容")
	private String plainTextContent;
}
