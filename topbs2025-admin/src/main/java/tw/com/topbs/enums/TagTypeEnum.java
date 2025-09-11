package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TagTypeEnum {
	MEMBER("member", "memberTagStrategy"),
	ATTENDEES("attendees", "attendeesTagStrategy"),
	PAPER("paper", "paperTagStrategy"),
	PAPER_REVIEWER("paper-reviewer", "paperReviewerTagStrategy");

	private final String type;
	private final String strategy;

	public static TagTypeEnum fromType(String value) {
		for (TagTypeEnum tagTypeEnum : values()) {
			if (tagTypeEnum.type.equals(value))
				return tagTypeEnum;
		}
		throw new IllegalArgumentException("無效的Tag類型: " + value);
	}

}
