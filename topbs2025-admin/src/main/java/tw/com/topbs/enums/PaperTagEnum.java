package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaperTagEnum {
	/**
	 * ALL，是給所有投稿都設置的Tag
	 */
	ALL("P"),
	/**
	 * ACCEPTED，是給摘要「通過」審核的稿件,設置的Tag
	 */
	ACCEPTED("Accepted"),
	/**
	 * REJECTED，是給摘要「未通過」審核的稿件,設置的Tag
	 */
	REJECTED("Rejected"),
	/**
	 * NOT_SUBMITTED_SLIDE，是給摘要「通過」審核的稿件,但未上傳slide/poster/video檔案,設置的Tag<br>
	 * 業務邏輯上，它會在稿件狀態變為 ACCEPTED,一併賦予此Tag
	 */
	NOT_SUBMITTED_SLIDE("Not-Submitted-Slide"),
	/**
	 * AWARDED，在需要提前決出稿件得獎者的情況使用<br>
	 * 是給「得獎」的稿件設置的 Tag<br>
	 * 基本上還是得額外寄信,因為獎項可能很複雜,這只能告知他有得獎
	 */
	AWARDED("Awarded"),
	/**
	 * NOT_AWARDED，在需要提前決出稿件得獎者的情況使用<br>
	 * 是給「未得獎」的稿件設置的 Tag<br>
	 * 基本上還是得額外寄信
	 */
	NOT_AWARDED("Not-Awarded");

	private final String tagName;

//	public static PaperTagEnum fromTagName(String tagName) {
//		for (PaperTagEnum type : values()) {
//			if (type.tagName.equals(tagName))
//				return type;
//		}
//		throw new IllegalArgumentException("無效的稿件 Tag: " + tagName);
//	}

}
