package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class AddPaperReviewerDTO {

	// 確保 stringList 不為 null 且至少包含一個元素。
	@NotEmpty
	@Schema(description = "評審類別,可用,號分隔, 表示可以審多個領域的Paper")
	private List<String> absTypeList;

	@Schema(description = "評審姓名")
	private String name;

	// 確保 stringList 不為 null 且至少包含一個元素。
	@NotEmpty
	@Schema(description = "評審聯繫信箱,多個信箱可用,號 分隔")
	private List<String> emailList;

	@Schema(description = "評審電話")
	private String phone;


}
