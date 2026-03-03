package tw.com.topbs.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ResponseAnswerVO {
	
	@Schema(description = "主鍵ID")
	private Long responseAnswerId;

	@Schema(description = "表單回覆紀錄ID")
	private Long formResponseId;

	@Schema(description = "表單欄位ID")
	private Long formFieldId;

	@Schema(description = "回覆值")
	private String answerValue;

}
