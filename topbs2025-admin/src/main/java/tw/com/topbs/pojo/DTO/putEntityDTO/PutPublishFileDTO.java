package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PutPublishFileDTO {

	@NotNull
	@Schema(description = "檔案表，主鍵ID")
	private Long publishFileId;

	@Schema(description = "群組類型，用於分別是屬於哪個頁面的檔案")
	@NotBlank
	private String groupType;

	@Schema(description = "二級類別,如果群組類別底下還有細分類別,可以用這個")
	private String type;

	@Schema(description = "檔名")
	@NotBlank
	private String name;

	@Schema(description = "檔案描述")
	private String description;

	@Schema(description = "外部鏈結")
	private String link;

	@Schema(description = "排序值")
	@NotNull
	private Integer sort;

	@Schema(description = "儲存地址")
	private String path;

	@Schema(description = "檔案封面縮圖URL")
	private String coverThumbnailUrl;

}
