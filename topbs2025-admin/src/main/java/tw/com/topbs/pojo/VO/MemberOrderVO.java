package tw.com.topbs.pojo.VO;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.Orders;

@Data
public class MemberOrderVO {
	
	@Schema(description = "主鍵ID")
	private Long memberId;

	@Schema(description = "名字, 華人的名在後  , 外國人的名在前")
	private String firstName;

	@Schema(description = "姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	private String lastName;

	@Schema(description = "E-Mail")
	private String email;

	@Schema(description = "國家")
	private String country;

	@Schema(description = "用於分類會員資格, 1為 Invited Speaker 、 2為 Board Member 、 3為 Normal Member 、 4為 Companion")
	private Integer category;

	@Schema(description = "單位(所屬的機構)")
	private String affiliation;

	@Schema(description = "職稱")
	private String jobTitle;

	@Schema(description = "電話號碼,這邊要使用 國碼-號碼")
	private String phone;
	
	@Schema(description = "會員持有的訂單列表")
	private List<Orders> ordersList;
}
