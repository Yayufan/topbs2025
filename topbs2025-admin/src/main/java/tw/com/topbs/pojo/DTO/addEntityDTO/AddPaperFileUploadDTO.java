package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddPaperFileUploadDTO {

	@Schema(description = "PDF檔案路徑")
	private String abstractFilePdfUrl;
	
	@Schema(description = "Word檔案路徑")
	private String abstractFileWordUrl;
	
	@Schema(description = "公文檔案01路徑")
	private String officialDocumentFileUrl01;
	
	@Schema(description = "公文檔案02路徑")
	private String officialDocumentFileUrl02;
	
	@Schema(description = "公文檔案03路徑")
	private String officialDocumentFileUrl03;
	
	@Schema(description = "Slide上傳路徑")
	private String slideUploadUrl;
	
}
