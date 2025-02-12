package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddPaperReviewerDTO {

	@Schema(description = "評審類別,可用，號分隔, 表示可以審多個領域的Paper")
	private String absTypeList;
	
	@Schema(description = "評審姓名")
	private String name;
	
	@Schema(description = "評審聯繫信箱,多個信箱可用；號 ,分隔")
	private String email;
	
	@Schema(description = "評審電話")
	private String phone;
	
	@Schema(description = "評審帳號")
	private String account;
	
	@Schema(description = "評審密碼")
	private String password;
	
	@Schema(description = "評審公文檔案01")
	private String officialDocumentFileUrl01;
	
	@Schema(description = "評審公文檔案02")
	private String officialDocumentFileUrl02;
	
	@Schema(description = "評審公文檔案03")
	private String officialDocumentFileUrl03;
	
}
