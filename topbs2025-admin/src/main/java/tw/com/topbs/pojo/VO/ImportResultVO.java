package tw.com.topbs.pojo.VO;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ImportResultVO {

	@Schema(description = "Excel總行數")
	private int totalCount;

	@Schema(description = "成功更新數量")
	private int successCount;

	@Schema(description = "失敗數量")
	private int failCount;

	@Schema(description = "失敗詳細列表")
	private List<FailDetail> failList = new ArrayList<>();

	@Data
	@AllArgsConstructor
	public static class FailDetail {

		@Schema(description = "失敗行號")
		private int row;

		@Schema(description = "失敗錯誤訊息")
		private String message;
	}
}
