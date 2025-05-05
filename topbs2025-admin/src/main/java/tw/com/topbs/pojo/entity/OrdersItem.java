package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 訂單細項表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("orders_item")
@Schema(name = "OrdersItem", description = "訂單細項表")
public class OrdersItem implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("orders_item_id")
	private Long ordersItemId;

	@Schema(description = "訂單ID")
	@TableField("orders_id")
	private Long ordersId;

	@Schema(description = "產品類型")
	@TableField("product_type")
	private String productType;

	@Schema(description = "產品名稱")
	@TableField("product_name")
	private String productName;

	@Schema(description = "入住時間")
	@TableField("check_in_date")
	private LocalDate checkInDate;

	@Schema(description = "退房時間")
	@TableField("check_out_date")
	private LocalDate checkOutDate;

	@Schema(description = "單價")
	@TableField("unit_price")
	private BigDecimal unitPrice;

	@Schema(description = "數量")
	@TableField("quantity")
	private Integer quantity;

	@Schema(description = "折扣")
	@TableField("discount")
	private BigDecimal discount;

	@Schema(description = "小計")
	@TableField("subtotal")
	private BigDecimal subtotal;

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
