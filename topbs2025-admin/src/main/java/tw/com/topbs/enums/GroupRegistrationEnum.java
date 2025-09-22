package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 對標 member table , category 屬性
 * 
 */
@Getter
@AllArgsConstructor
public enum GroupRegistrationEnum {
	/**
	 * value 為 master<br>
	 * label 為 團體報名負責人
	 */
	MASTER("master", "團體報名負責人"),
	/**
	 * value 為 slave<br>
	 * label 為 團體成員
	 */
	SLAVE("slave", "團體成員");

	/**
	 * 儲存值
	 */
	private final String value;
	/**
	 * 顯示標籤
	 */
	private final String label;

	//	public static GroupRegistrationEnum fromValue(Integer value) {
	//		for (GroupRegistrationEnum type : values()) {
	//			if (type.value.equals(value))
	//				return type;
	//		}
	//		throw new IllegalArgumentException("無效的會員身份值: " + value);
	//	}

}
