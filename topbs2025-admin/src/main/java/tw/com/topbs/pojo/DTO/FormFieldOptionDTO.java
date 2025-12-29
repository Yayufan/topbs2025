package tw.com.topbs.pojo.DTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "表單欄位選項設定")
@Data
public class FormFieldOptionDTO {

	@Schema(description = "可選項目")
	@NotNull
	@Size(min = 1, message = "至少要有一個選項")
	@Valid
	private List<Choice> choices;

	// 基本類型布爾值,沒傳入,預設是false
	@Schema(description = "是否允許自訂輸入")
	private boolean allowCustom;

	@Data
	public static class Choice {

		@Schema(description = "顯示文字")
		@NotBlank
		private String label;

		@Schema(description = "實際值")
		@NotNull
		private String value;
	}

}
