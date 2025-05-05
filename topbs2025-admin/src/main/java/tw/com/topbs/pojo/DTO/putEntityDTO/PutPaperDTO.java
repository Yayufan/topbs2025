package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutPaperDTO {

	@NotNull
	@Schema(description = "稿件ID")
	private Long paperId;

	@NotNull
	@Schema(description = "會員ID")
	private Long memberId;

	@Schema(description = "報告方式")
	private String presentationType;

	@NotBlank
	@Schema(description = "投稿類別")
	private String absType;

	@Schema(description = "文章屬性")
	private String absProp;

	@NotBlank
	@Schema(description = "稿件主題_國際會議所以只收英文")
	private String absTitle;

	@NotBlank
	@Schema(description = "第一作者")
	private String firstAuthor;

	@Schema(description = "第一作者生日，用來判斷是否符合獎項資格")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate firstAuthorBirthday;

	@NotBlank
	@Schema(description = "主講者")
	private String speaker;

	@NotBlank
	@Schema(description = "主講者單位_國際會議所以只收英文")
	private String speakerAffiliation;

	@NotBlank
	@Schema(description = "通訊作者")
	private String correspondingAuthor;

	@NotBlank
	@Schema(description = "通訊作者E-Mail")
	private String correspondingAuthorEmail;

	@NotBlank
	@Schema(description = "通訊作者聯絡電話")
	private String correspondingAuthorPhone;

	@NotBlank
	@Schema(description = "全部作者")
	private String allAuthor;

	@NotBlank
	@Schema(description = "全部作者單位")
	private String allAuthorAffiliation;

}
