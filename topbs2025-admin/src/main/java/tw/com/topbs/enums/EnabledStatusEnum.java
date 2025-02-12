package tw.com.topbs.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public enum EnabledStatusEnum {
	ENABLED("1", "啟用"), 
	DISABLED("0", "禁用");

	private final String code;
	private final String description;
	
	// 根據代碼獲取對應的枚舉常量
    public static EnabledStatusEnum fromCode(String code) {
        return Arrays.stream(EnabledStatusEnum.values())
                     .filter(status -> status.code.equals(code))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("無效的狀態代碼: " + code));
    }

}
