package tw.com.topbs.pojo.VO;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.Tag;

@Data
public class PaperVO {

	@Schema(description = "主鍵ID")
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

	@Schema(description = "主講者")
	private String speaker;

	@Schema(description = "主講者單位_國際會議所以只收英文")
	private String speakerAffiliation;

	@Schema(description = "通訊作者")
	private String correspondingAuthor;

	@Schema(description = "通訊作者E-Mail")
	private String correspondingAuthorEmail;

	@Schema(description = "通訊作者聯絡電話")
	private String correspondingAuthorPhone;

	@Schema(description = "全部作者")
	private String allAuthor;

	@Schema(description = "全部作者單位")
	private String allAuthorAffiliation;

	@Schema(description = "稿件狀態,預設為0未審核,1為已入選,2為未入選")
	private Integer status;

	@Schema(description = "發表編號")
	private String publicationNumber;

	@Schema(description = "發表組別")
	private String publicationGroup;

	@Schema(description = "報告地點")
	private String reportLocation;

	@Schema(description = "報告時間")
	private String reportTime;

	@Schema(description = "附件資訊")
	private List<PaperFileUpload> paperFileUpload;

	@Schema(description = "可選擇的，稿件評審人員")
	private List<PaperReviewer> availablePaperReviewers;

	@Schema(description = "實際分配的，稿件評審人員")
	private List<PaperReviewer> assignedPaperReviewers;
	
	@Schema(description = "持有的標籤")
	private Set<Tag> tagSet;
}
