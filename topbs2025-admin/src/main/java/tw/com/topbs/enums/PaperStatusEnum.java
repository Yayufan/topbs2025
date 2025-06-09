package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaperStatusEnum {
	UNREVIEWED(0, "Unreviewed", "未審核"),
	ACCEPTED(1, "Accepted", "入選"),
	REJECTED(2, "Rejected", "未入選"),
	ACCEPTED_STAGE_2(3, "Accepted (Stage 2)","入選(二階段)"),
	REJECTED_STAGE_2(4, "Rejected (Stage 2)", "未入選(二階段)");

	private final Integer value;
	private final String labelEn;
	private final String labelZh;

	public static PaperStatusEnum fromValue(Integer value) {
		for (PaperStatusEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的稿件狀態類型值: " + value);
	}

}
