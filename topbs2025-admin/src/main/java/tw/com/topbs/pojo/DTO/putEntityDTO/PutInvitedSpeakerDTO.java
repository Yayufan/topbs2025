package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutInvitedSpeakerDTO {

	@Schema(description = "主鍵ID")
	private Long invitedSpeakerId;
	
	@Schema(description = "會員ID")
	private Long memberId;

	@Schema(description = "講者的全名")
	private String name;

	@Schema(description = "講者的國籍")
	private String country;

	@Schema(description = "講者現任職的單位")
	private String affiliation;

	@Schema(description = "教育背景/學歷，用Json array處理")
	private List<String> educationalBackground;

	@Schema(description = "工作經歷")
	private List<String> workExperience;

	@Schema(description = "發表過的期刊/書籍/作品/研究")
	private List<String> publication;

	@Schema(description = "得到過的，榮譽或獎項")
	private List<String> award;
	
	@Schema(description = "發佈狀態 ; 0為草稿(待發佈),1為已發佈")
	private Integer isPublished;

}
