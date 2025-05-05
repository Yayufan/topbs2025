package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 訂單表
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Getter
@Setter
@TableName("orders")
@Schema(name = "Orders", description = "訂單表")
public class Orders implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("orders_id")
	private Long ordersId;

	@Schema(description = "會員ID")
	@TableField("member_id")
	private Long memberId;
	
	@Schema(description = "這筆訂單商品的統稱- 因為綠界沒有辦法顯示 明細 對應 細項金額,只能統整Total金額")
	@TableField("items_summary")
	private String itemsSummary;
	
	@Schema(description = "訂單總金額")
	@TableField("total_amount")
	private BigDecimal totalAmount;

	@Schema(description = "訂單狀態 0為未付款 ; 1為已付款-待審核 ; 2為付款成功 ; 3為付款失敗")
	@TableField("status")
	private Integer status;

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
