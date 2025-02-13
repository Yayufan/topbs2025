package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name="data",description = "AddPaperDTO")
public class AddPaperDTO {
	
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
	

		
	
}
