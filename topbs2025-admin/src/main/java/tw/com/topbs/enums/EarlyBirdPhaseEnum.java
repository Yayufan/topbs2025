package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EarlyBirdPhaseEnum {
	PHASE_ONE("早鳥優惠-第一階段"),
	PHASE_TWO("早鳥優惠-第二階段"),
	PHASE_THREE("早鳥優惠-第三階段"),
	NONE("無早鳥優惠"); // 當前不在任何階段


	/**
	 * 早鳥階段-描述
	 */
	private final String description;


//	public static EarlyBirdPhaseEnum fromValue(Integer value) {
//		for (EarlyBirdPhaseEnum type : values()) {
//			if (type.description.equals(value))
//				return type;
//		}
//		throw new IllegalArgumentException("無效的值: " + value);
//	}
}
