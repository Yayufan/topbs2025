package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddOrdersItemDTO {
	
	@Schema(description = "訂單ID")
	private Long ordersId;

	@Schema(description = "產品名稱")
	private String productName;

	@Schema(description = "產品類型")
	private String productType;

	@Schema(description = "數量")
	private Integer quantity;

	@Schema(description = "單價")
	private BigDecimal unitPrice;

	@Schema(description = "折扣")
	private BigDecimal discount;

	@Schema(description = "小計")
	private BigDecimal subtotal;

	@Schema(description = "入住時間")
	private LocalDate checkInDate;

	@Schema(description = "退房時間")
	private LocalDate checkOutDate;
}
