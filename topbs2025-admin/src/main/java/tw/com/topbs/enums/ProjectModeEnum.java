package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import tw.com.topbs.exception.PaperAbstractsException;

@Getter
@AllArgsConstructor
@ToString
public enum ProjectModeEnum {

	FREE("free", "免費模式，所有功能都可以直接使用","freeModeStrategy"), 
	PREPAID("prepaid", "先付模式，使用功能前需先付款","prepaidModeStrategy"),
	POSTPAID("postpaid", "後付模式，使用功能前需先付款","postpaidModeStrategy");

	private final String value; // 值
	private final String description;  // 簡述
	private final String registrationStrategyKey;

	public static ProjectModeEnum fromValue(String value) {
		for (ProjectModeEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new PaperAbstractsException("無效的 模式 " + value);
	}

}
