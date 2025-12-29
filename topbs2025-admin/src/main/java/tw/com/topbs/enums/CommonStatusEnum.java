package tw.com.topbs.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum CommonStatusEnum {

	NO(0, false, "否/禁用"), YES(1, true, "是/啟用");

	@JsonValue
	@EnumValue
	private final Integer value;

	private final Boolean booleanValue;

	private final String description;

	// 根據 值 獲取對應的枚舉常量
	@JsonCreator
	public static CommonStatusEnum fromValue(Integer value) {
		for (CommonStatusEnum commonStatusEnum : values()) {
			if (commonStatusEnum.value.equals(value))
				return commonStatusEnum;
		}
		throw new IllegalArgumentException("無效的值: " + value);
	}

}
