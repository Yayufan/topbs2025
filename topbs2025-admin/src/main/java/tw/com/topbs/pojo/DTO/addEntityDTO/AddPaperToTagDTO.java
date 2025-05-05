package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddPaperToTagDTO {

	// 確保 `targetPaperIdList` 不能是 `null`
	// 不使用 @Size(min = 1)，這樣允許空列表 `[]`
	@NotNull(message = "稿件 ID 清單不能為null")
	private List<Long> targetPaperIdList;

	@NotNull(message = "標籤 ID 不能為null")
	private Long tagId;
}
