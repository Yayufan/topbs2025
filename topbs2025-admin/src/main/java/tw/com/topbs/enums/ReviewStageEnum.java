package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import tw.com.topbs.exception.PaperAbstractsException;

@Getter
@AllArgsConstructor
@ToString
public enum ReviewStageEnum {

	FIRST_REVIEW("first_review", "第一階段審核 摘要","R1","Not-Review-Finish-R1"),
	SECOND_REVIEW("second_review", "第二階段審核 稿件","R2","Not-Review-Finish-R2");


	private final String value; // 值
	private final String label;  // 簡述
	private final String tagPrefix; // 新增字段，表示對應的 Tag 名稱前綴
	private final String notReviewTagPrefix;

	public static ReviewStageEnum fromValue(String value) {
		for (ReviewStageEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new PaperAbstractsException("無效的 審核階段 " + value);
	}

}
