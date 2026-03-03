package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RegistrationPhaseEnum {
	PHASE_ONE("early-bird-phase1","早鳥優惠-第一階段"),
	PHASE_TWO("early-bird-phase2","早鳥優惠-第二階段"),
	PHASE_THREE("early-bird-phase3","早鳥優惠-第三階段"),
	REGULAR("regular","一般階段，無優惠"),
	ON_SITE("on-site","現場註冊");

	
	private final String value;
	
	/**
	 * 早鳥階段-描述
	 */
	private final String description;


//	public static RegistrationPhaseEnum fromValue(Integer value) {
//		for (RegistrationPhaseEnum type : values()) {
//			if (type.description.equals(value))
//				return type;
//		}
//		throw new IllegalArgumentException("無效的值: " + value);
//	}
}
