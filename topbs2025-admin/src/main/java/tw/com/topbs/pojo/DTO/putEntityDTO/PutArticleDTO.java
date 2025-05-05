package tw.com.topbs.pojo.DTO.putEntityDTO;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutArticleDTO {

	@NotNull
	@Schema(description = "主鍵ID")
	private Long articleId;


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

	@Schema(description = "HTML文章內容")
	private String content;
	
	@NotNull
	@Schema(description = "暫時上傳圖片的URL")
	private List<String> tempUploadUrl;

}
