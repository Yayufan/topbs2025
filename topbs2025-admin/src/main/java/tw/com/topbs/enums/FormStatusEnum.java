package tw.com.topbs.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定義表單狀態
 */
@Getter
@AllArgsConstructor
public enum FormStatusEnum {
	DRAFT("draft", "草稿"), PUBLISHED("published", "發佈"), CLOSED("closed", "關閉");

	@EnumValue
	@JsonValue // JSON 輸出用
	private final String value;

	private final String label;

	/**
	 * 從value獲取 Enum
	 * 
	 * @param value
	 * @return
	 */
	@JsonCreator // JSON 輸入用
	public static FormStatusEnum fromValue(String value) {
		for (FormStatusEnum formStatus : values()) {
			if (formStatus.value.equals(value))
				return formStatus;
		}
		throw new IllegalArgumentException("無效的表單狀態: " + value);
	}

	/**
	 * 從label獲取 Enum
	 * 
	 * @param label
	 * @return
	 */
	public static FormStatusEnum fromLabel(String label) {
		for (FormStatusEnum formStatus : values()) {
			if (formStatus.label.equals(label))
				return formStatus;
		}
		throw new IllegalArgumentException("無效的表單狀態: " + label);
	}

}
