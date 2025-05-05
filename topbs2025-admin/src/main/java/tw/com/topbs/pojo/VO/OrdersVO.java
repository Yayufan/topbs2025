package tw.com.topbs.pojo.VO;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class OrdersVO {

	@Schema(description = "主鍵ID")
	private Long ordersId;
	
	@Schema(description = "自然鍵")
	private byte[] natureId;
	
	@Schema(description = "訂單總金額")
	private BigDecimal totalAmount;

	@Schema(description = "訂單狀態 0為未付款 1為已付款 2為付款失敗")
	private Integer status;

}
