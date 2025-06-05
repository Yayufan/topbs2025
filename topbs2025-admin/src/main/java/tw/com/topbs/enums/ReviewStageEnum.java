package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReviewStageEnum {

	FIRST_REVIEW("first_review", "第一階段審核"), SECOND_REVIEW("second_review", "第二階段審核"),
	THIRD_REVIEW("third_review", "第三階段審核");

	private final String value;
	private final String label;

	public static ReviewStageEnum fromValue(String value) {
		for (ReviewStageEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的 審核階段 值: " + value);
	}

}
