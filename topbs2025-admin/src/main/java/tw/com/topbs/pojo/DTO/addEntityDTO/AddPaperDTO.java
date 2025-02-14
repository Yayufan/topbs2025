package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "data", description = "AddPaperDTO")
public class AddPaperDTO {

	@NotNull
	@Schema(description = "會員ID")
	private Long memberId;

	@NotBlank
	@Schema(description = "投稿類別")
	private String absType;

	@NotBlank
	@Schema(description = "文章屬性")
	private String absProp;

	@NotBlank
	@Schema(description = "稿件主題_國際會議所以只收英文")
	private String absTitle;

	@NotBlank
	@Schema(description = "第一作者")
	private String firstAuthor;

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
