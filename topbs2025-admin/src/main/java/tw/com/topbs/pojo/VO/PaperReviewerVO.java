package tw.com.topbs.pojo.VO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.pojo.entity.Tag;

@Data
public class PaperReviewerVO {
	
	@Schema(description = "主鍵ID")
	private Long paperReviewerId;

	@Schema(description = "評審姓名")
	private String name;

	@Schema(description = "評審類別,可用 , 號分隔,表示可以審多個領域的Paper")
	private List<String> absTypeList;

	@Schema(description = "評審聯繫信箱,多個信箱可用,號 分隔")
	private List<String> emailList;

	@Schema(description = "評審電話")
	private String phone;

	@Schema(description = "評審帳號")
	private String account;

	@Schema(description = "評審密碼")
	private String password;

	@Schema(description = "持有的標籤")
	private List<Tag> tagList;
	
	@Schema(description = "持有的公文附件列表")
	private List<PaperReviewerFile> paperReviewerFileList;

}
