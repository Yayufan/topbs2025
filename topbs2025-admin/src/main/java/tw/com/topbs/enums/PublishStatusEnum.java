package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PublishStatusEnum {
	DRAFT(0, "草稿"),
	PUBLISHED(1, "已發佈");

	private final Integer value;
	private final String description;

	public static PublishStatusEnum fromValue(Integer value) {
		for (PublishStatusEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的狀態類型值: " + value);
	}

}
