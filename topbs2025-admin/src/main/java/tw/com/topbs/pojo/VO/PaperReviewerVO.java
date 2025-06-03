package tw.com.topbs.pojo.VO;

import java.util.List;

import com.baomidou.mybatisplus.annotation.TableId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
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

	@Schema(description = "評審 應 審核稿件數量")
	private Integer totalReviewCount;

	@Schema(description = "評審 已 審核稿件數量")
	private Integer completedReviewCount;

	@Schema(description = "持有的標籤")
	private List<Tag> tagList;

}
