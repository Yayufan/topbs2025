package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 對標 member table , category 屬性
 * 
 */
@Getter
@AllArgsConstructor
public enum ECpayRtnCodeEnum {

	SUCCESS("1", "付款成功"),
	PENDING("10300066", "交易付款結果待確認中，請勿出貨，請至廠商管理後台確認已付款完成再出貨"),
	REJECTED("10100248", "拒絕交易，請客戶聯繫發卡行確認原因"),
	INSUFFICIENT_FUNDS("10100252", "額度不足，請客戶檢查卡片額度或餘額"),
	FAILED("10100254", "交易失敗，請客戶聯繫發卡行確認交易限制"),
	CARD_EXPIRED("10100251", "卡片過期，請客戶檢查卡片重新交易"),
	CARD_REPORTED_LOST("10100255", "報失卡，請客戶更換卡片重新交易"),
	STOLEN_CARD("10100256", "被盜用卡，請客戶更換卡片重新交易"),
	UNKNOWN("", "未知的錯誤代碼"); // fallback

	private final String code;
	private final String message;
	
    /**
     * 根據回傳代碼取得對應的 Enum
     */
    public static ECpayRtnCodeEnum fromCode(String code) {
        for (ECpayRtnCodeEnum result : values()) {
            if (result.code.equals(code)) {
                return result;
            }
        }
        return UNKNOWN;
    }

    /**
     * 是否為付款成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

}
