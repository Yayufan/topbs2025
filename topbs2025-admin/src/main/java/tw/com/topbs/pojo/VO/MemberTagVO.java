package tw.com.topbs.pojo.VO;

import java.time.LocalDate;
import java.util.Set;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.entity.Tag;

@Data
public class MemberTagVO {

	@Schema(description = "會員ID")
	private Long memberId;

	@Schema(description = "信箱")
	private String email;

	@Schema(description = "名字")
	private String firstName;

	@Schema(description = "姓氏")
	private String lastName;

	@Schema(description = "國家")
	private String country;

	@Schema(description = "匯款帳號-後五碼  台灣會員使用")
	private String remitAccountLast5;

	@Schema(description = "單位(所屬的機構)")
	private String affiliation;

	@Schema(description = "職稱")
	private String jobTitle;

	@Schema(description = "電話號碼")
	private String phone;

	@Schema(description = "訂單狀態 0為未付款 ; 1為已付款-待審核 ; 2為付款成功 ; 3為付款失敗")
	private Integer status;

	@Schema(description = "持有的標籤")
	private Set<Tag> tagSet;

}
