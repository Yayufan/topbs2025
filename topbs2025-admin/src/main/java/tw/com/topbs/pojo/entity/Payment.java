package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 付款紀錄表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("payment")
@Schema(name = "Payment", description = "付款紀錄表")
public class Payment implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("payment_id")
	private Long paymentId;

	@Schema(description = "訂單ID")
	@TableField("orders_id")
	private Long ordersId;

	@Schema(description = "付款日期")
	@TableField("payment_date")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime paymentDate;

	@Schema(description = "支付方式")
	@TableField("payment_type")
	private String paymentType;

	@Schema(description = "交易手續費金額")
	@TableField("payment_type_charge_fee")
	private Integer paymentTypeChargeFee;

	@Schema(description = "交易狀態")
	@TableField("rtn_code")
	private String rtnCode;

	@Schema(description = "交易信息")
	@TableField("rtn_msg")
	private String rtnMsg;

	@Schema(description = "交易金額")
	@TableField("trade_amt")
	private Integer tradeAmt;

	@Schema(description = "交易日期")
	@TableField("trade_date")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime tradeDate;

	@Schema(description = "綠界交易編號")
	@TableField("trade_number")
	private String tradeNumber;

	@Schema(description = "是否為模擬付款;0：代表此交易非模擬付款。1：代表此交易為模擬付款，RtnCode也為1。並非是由消費者實際真的付款，所以綠界也不會撥款給廠商。")
	@TableField("simulate_paid")
	private Integer simulatePaid;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField(value = "update_by", fill = FieldFill.UPDATE)
	private String updateBy;

	@Schema(description = "最後修改時間")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;
}
