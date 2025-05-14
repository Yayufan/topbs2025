package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
	/**
	 * 未付款，代號為 0
	 * <p>
	 * Label為 "Unpaid", "未付款"
	 * <p>
	 * 訂單已建立，但尚未收到任何付款。
	 */
	UNPAID(0, "Unpaid", "未付款"),

	/**
	 * 付款待確認，代號為 1
	 * <p>
	 * Label為 "Pending-Confirmation", "付款-待確認"
	 * <p>
	 * 客戶已付款，但系統/審核人員，尚未確認收款。
	 */
	PENDING_CONFIRMATION(1, "Pending-Confirmation", "付款-待確認"),

	/**
	 * 付款成功，代號為 2
	 * <p>
	 * Label為 "Payment-Success", "付款完成"
	 * <p>
	 * 系統/審核人員，已確認收到客戶的付款。
	 */
	PAYMENT_SUCCESS(2, "Payment-Success", "付款完成"),

	/**
	 * 付款失敗，代號為 3
	 * <p>
	 * Label為 "Payment-Failed", "付款失败"
	 * <p>
	 * 客戶付款失敗或交易被拒絕。
	 */
	PAYMENT_FAILED(3, "Payment-Failed", "付款失败");

	/**
	 * DB table 中的代號
	 */
	private final Integer value;

	/**
	 * 標籤-英文
	 */
	private final String labelEn;

	/**
	 * 標籤-中文
	 */
	private final String labelZh;

	public static OrderStatusEnum fromValue(Integer value) {
		for (OrderStatusEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的付款值: " + value);
	}
}
