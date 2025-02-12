package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddPaymentDTO {

	@Schema(description = "支付方式")
	private String paymentType;

	@Schema(description = "交易狀態")
	private String rtnCode;

	@Schema(description = "交易信息")
	private String rtnMsg;

	@Schema(description = "綠界交易編號")
	private String tradeNumber;

	@Schema(description = "交易金額")
	private Integer tradeAmt;

	@Schema(description = "付款日期")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime paymentDate;

	@Schema(description = "交易手續費金額")
	private Integer paymentTypeChargeFee;

	@Schema(description = "交易日期")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime tradeDate;

	@Schema(description = "是否為模擬付款;0：代表此交易非模擬付款。"
			+ "1：代表此交易為模擬付款，RtnCode也為1。並非是由消費者實際真的付款，所以綠界也不會撥款給廠商，")
	private Integer simulatePaid;
}
