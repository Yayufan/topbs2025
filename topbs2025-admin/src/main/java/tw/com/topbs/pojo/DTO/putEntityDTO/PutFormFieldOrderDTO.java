package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutFormFieldOrderDTO  {

	@NotNull
	@Schema(description = "主鍵ID")
	private Long formFieldId;
	
	@NotNull
	@Schema(description = "顯示順序 , 數字越小排的越前面")
	private Integer fieldOrder;


}
