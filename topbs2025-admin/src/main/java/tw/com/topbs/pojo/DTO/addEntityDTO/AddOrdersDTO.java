package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddOrdersDTO {
	
	@NotNull
	@Schema(description = "會員ID")
	private Long memberId;
	
	@Schema(description = "這筆訂單商品的統稱- 因為綠界沒有辦法顯示 明細 對應 細項金額,只能統整Total金額")
	private String itemsSummary;

	@Schema(description = "訂單總金額")
	private BigDecimal totalAmount;

	@Schema(description = "訂單狀態 0為未付款 ; 1為已付款-待審核 ; 2為付款成功 ; 3為付款失敗")
	private Integer status;

}
