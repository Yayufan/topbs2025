package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FormFieldValidationRuleDTO {

	@Schema(description = "value 最大值")
	private Integer max;

	@Schema(description = "value 最小值")
	private Integer min;

	@Schema(description = "顯示條件")
	@Valid
	private ShowIf showIf;

	@Data
	public static class ShowIf {

		@Schema(description = "依賴的問題 ID")
		@NotNull
		private Long fieldId;

		@Schema(description = "依賴的問題 觸發顯示的值")
		@NotBlank
		private String value;
	}

}
