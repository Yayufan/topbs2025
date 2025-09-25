package tw.com.topbs.pojo.DTO;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddAssociatedToTagDTO {

	// 確保 `targetAssociatedIdList` 不能是 `null`
	// 不使用 @Size(min = 1)，這樣允許空列表 `[]`
	@NotNull(message = "關聯 ID 清單不能為null")
	private List<Long> targetAssociatedIdList;

	@NotNull(message = "標籤 ID 不能為null")
	private Long tagId;
	
}
