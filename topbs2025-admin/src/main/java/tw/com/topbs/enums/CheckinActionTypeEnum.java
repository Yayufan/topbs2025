package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 對標 checkinRecord table , action_type 屬性
 * 
 */
@Getter
@AllArgsConstructor
public enum CheckinActionTypeEnum {
	CHECKIN(1, "簽到"), CHECKOUT(2, "簽退");

	private final Integer value;
	private final String label;

	public static CheckinActionTypeEnum fromValue(Integer value) {
		for (CheckinActionTypeEnum type : values()) {
			if (type.value == value)
				return type;
		}
		throw new IllegalArgumentException("無效的簽到行為值: " + value);
	}
}
