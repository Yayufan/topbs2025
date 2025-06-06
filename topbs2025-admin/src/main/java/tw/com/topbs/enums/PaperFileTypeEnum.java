package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaperFileTypeEnum {
	// 摘要檔案-PDF檔
	ABSTRUCTS_PDF("abstructs_pdf"),
	// 摘要檔案-WORD檔
	ABSTRUCTS_DOCX("abstructs_docx"),
	 // 第二階段補充資料 (包含簡報、影片、海報檔等)
    SUPPLEMENTARY_MATERIAL("supplementary_material");

	private String value;

	public static PaperFileTypeEnum fromValue(String value) {
		for (PaperFileTypeEnum type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的檔案類型值: " + value);
	}

}
