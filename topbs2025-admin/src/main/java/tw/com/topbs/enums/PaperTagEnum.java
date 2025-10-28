package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaperTagEnum {
	ALL("P"),
	ACCEPTED_1("Accepted-1"),
	ACCEPTED_2("Accepted-2"),
	REJECTED_1("Rejected-1"),
	REJECTED_2("Rejected-2");

	private final String tagName;

//	public static PaperTagEnum fromTagName(String tagName) {
//		for (PaperTagEnum type : values()) {
//			if (type.tagName.equals(tagName))
//				return type;
//		}
//		throw new IllegalArgumentException("無效的稿件 Tag: " + tagName);
//	}

}
