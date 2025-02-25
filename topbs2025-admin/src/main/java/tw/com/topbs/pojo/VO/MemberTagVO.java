package tw.com.topbs.pojo.VO;

import java.time.LocalDate;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.Tag;

@Data
public class MemberTagVO {

	private Long memberId;

	@Schema(description = "社交帳號登入提供商")
	private String provider;

	@Schema(description = "社交帳號使用者ID")
	private String providerUserId;

	@Schema(description = "姓名")
	private String name;

	@Schema(description = "信箱")
	private String email;

	@Schema(description = "院所(部門)")
	private String department;

	@Schema(description = "職稱")
	private String jobTitle;

	@Schema(description = "性別")
	private String gender;

	@Schema(description = "性別補充")
	private String genderOther;

	@Schema(description = "國民身分證字號/居留證")
	private String idCard;

	@JsonFormat(pattern = "yyyy-MM-dd")
	@Schema(description = "出生年月日")
	private LocalDate birthday;

	@Schema(description = "聯絡地址")
	private String contactAddress;

	@Schema(description = "電話號碼")
	private String phone;
	
	@Schema(description = "會員編號 , 顯示給用戶時要加HA")
	private Integer code;
	
	@Schema(description = "狀態,0為待審核,1為審核通過,2為審核不通過")
	private String status;
	
	@Schema(description = "持有的標籤")
	private Set<Tag> tagSet;
	
}
