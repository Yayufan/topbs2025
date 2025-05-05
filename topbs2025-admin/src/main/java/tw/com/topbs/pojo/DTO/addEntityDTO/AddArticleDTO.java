package tw.com.topbs.pojo.DTO.addEntityDTO;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddArticleDTO {

	@Schema(description = "群組 - 用於分類文章的群組")
	private String groupType;

	@Schema(description = "同步ID")
	private Long asyncId;

	@Schema(description = "類別ID")
	private Long categoryId;

	@Schema(description = "文章的類型")
	private String type;

	@Schema(description = "文章標題")
	private String title;

	@Schema(description = "對文章的描述")
	private String description;

	@Schema(description = "公告此文章的日期")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate announcementDate;

	
}
