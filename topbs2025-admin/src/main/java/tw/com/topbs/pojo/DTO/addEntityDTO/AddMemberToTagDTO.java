package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberToTagDTO {
	
	// 確保 `targetMemberIdList` 不能是 `null`
	// 不使用 @Size(min = 1)，這樣允許空列表 `[]`
	@NotNull(message = "會員 ID 清單不能為null")
	private List<Long> targetMemberIdList;

	@NotNull(message = "標籤 ID 不能為null")
	private Long tagId;
}
