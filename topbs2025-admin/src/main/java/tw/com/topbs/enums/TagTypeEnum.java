package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TagTypeEnum {
	MEMBER("member", "memberTagStrategy","memberMailStrategy"),
	ATTENDEES("attendees", "attendeesTagStrategy","attendeesMailStrategy"),
	PAPER("paper", "paperTagStrategy","paperMailStrategy"),
	PAPER_REVIEWER("paper-reviewer", "paperReviewerTagStrategy","paperReviewerMailStrategy");

	private final String type;
	private final String tagStrategy;
	private final String mailStrategy;

	public static TagTypeEnum fromType(String value) {
		for (TagTypeEnum tagTypeEnum : values()) {
			if (tagTypeEnum.type.equals(value))
				return tagTypeEnum;
		}
		throw new IllegalArgumentException("無效的Tag類型: " + value);
	}

}
