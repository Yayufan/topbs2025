package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
	UNPAID(0, "Unpaid", "未付款"),
	PENDING_CONFIRMATION(1, "Pending-Confirmation", "付款-待確認"),
	PAYMENT_SUCCESS(2, "Payment-Success", "付款完成"),
	PAYMENT_FAILED(3, "Payment-Failed", "付款失败");
	

	private final Integer value;
	private final String labelEn;
	private final String labelZh;

	public static OrderStatusEnum fromValue(Integer value) {
		for (OrderStatusEnum type : values()) {
			if (type.value == value)
				return type;
		}
		throw new IllegalArgumentException("無效的付款值: " + value);
	}
}
