package tw.com.topbs.pojo.excelPojo;

import java.time.LocalDate;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class PaperScoreExcel {

	@ExcelProperty("主鍵ID")
	private String paperId;

	@ExcelProperty("會員ID")
	private String memberId;

	@ExcelProperty("稿件主題")
	private String absTitle;

	@ExcelProperty("第一作者")
	private String firstAuthor;

	@ExcelProperty("所有評審")
	private String allReviewers;

	@ExcelProperty("評分評審")
	private String scorers;

	@ExcelProperty("所有分數")
	private String AllScores;

	@ExcelProperty("平均分數")
	private Double averageScore;

	@ExcelProperty("報告方式")
	private String presentationType;

	@ExcelProperty("投稿類別")
	private String absType;

	@ExcelProperty("文章屬性")
	private String absProp;

	@ExcelProperty("第一作者生日")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate firstAuthorBirthday;

	@ExcelProperty("主講者")
	private String speaker;

	@ExcelProperty("主講者單位")
	private String speakerAffiliation;

	@ExcelProperty("通訊作者")
	private String correspondingAuthor;

	@ExcelProperty("通訊作者E-Mail")
	private String correspondingAuthorEmail;

	@ExcelProperty("通訊作者聯絡電話")
	private String correspondingAuthorPhone;

	@ExcelProperty("全部作者")
	private String allAuthor;

	@ExcelProperty("全部作者單位")
	private String allAuthorAffiliation;

	@ExcelProperty("稿件狀態")
	private String status;

	@ExcelProperty("發表編號")
	private String publicationNumber;

	@ExcelProperty("發表組別")
	private String publicationGroup;

	@ExcelProperty("報告地點")
	private String reportLocation;

	@ExcelProperty("報告時間")
	private String reportTime;

}
