package tw.com.topbs.pojo.VO;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.PaperFileUpload;

@Data
public class ReviewVO {

	@Schema(description = "評分表ID")
	private Long paperAndPaperReviewerId;

	@Schema(description = "稿件ID")
	private Long paperId;

	@Schema(description = "會員ID")
	private Long memberId;

	@Schema(description = "投稿類別")
	private String absType;

	@Schema(description = "文章屬性")
	private String absProp;

	@Schema(description = "稿件主題_國際會議所以只收英文")
	private String absTitle;

	@Schema(description = "第一作者")
	private String firstAuthor;

	@Schema(description = "第一作者生日，用來判斷是否符合獎項資格")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate firstAuthorBirthday;

	@Schema(description = "主講者")
	private String speaker;

	@Schema(description = "主講者單位_國際會議所以只收英文")
	private String speakerAffiliation;

	@Schema(description = "全部作者")
	private String allAuthor;

	@Schema(description = "全部作者單位")
	private String allAuthorAffiliation;

	@Schema(description = "附件資訊")
	private List<PaperFileUpload> fileList;

	@Schema(description = "分數")
	private Integer score;

}
