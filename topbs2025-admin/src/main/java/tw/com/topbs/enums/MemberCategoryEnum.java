package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 對標 member table , category 屬性
 * 
 */
@Getter
@AllArgsConstructor
public enum MemberCategoryEnum {
	MEMBER(1,"member", "Member", "Member(會員)"),
	OTHERS(2,"others", "Others", "Others(非會員)"),
	NON_MEMBER(3,"non-member", "Non-Member", "Non-Member(非會員醫師)"),
	MVP(4,"mvp", "MVP", "MVP"),
	SPEAKER(5,"speaker", "Speaker", "講者"),
	MODERATOR(6,"moderator", "Moderator", "座長"),
	STAFF(7,"staff" ,"Staff", "工作人員");

	private final Integer value;
	private final String configKey;
	private final String labelEn;
	private final String labelZh;

	public static MemberCategoryEnum fromValue(Integer value) {
		for (MemberCategoryEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的會員身份值: " + value);
	}

}
