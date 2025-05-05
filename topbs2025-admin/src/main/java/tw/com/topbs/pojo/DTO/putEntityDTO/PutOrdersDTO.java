package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutOrdersDTO {

	@Schema(description = "主鍵ID")
	private Long ordersId;
	
	@Schema(description = "訂單總金額")
	private BigDecimal totalAmount;

	@Schema(description = "訂單狀態 0為未付款 ; 1為已付款-待審核 ; 2為付款成功 ; 3為付款失敗")
	private Integer status;

}
