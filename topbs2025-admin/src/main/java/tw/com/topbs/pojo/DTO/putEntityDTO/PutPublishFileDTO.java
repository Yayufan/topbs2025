package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutPublishFileDTO {

	@Schema(description = "檔案表，主鍵ID")
	private Long publishFileId;
	
	@Schema(description = "群組類型，用於分別是屬於哪個頁面的檔案")
	private String groupType;

	@Schema(description = "二級類別,如果群組類別底下還有細分類別,可以用這個")
	private String type;

	@Schema(description = "檔名")
	private String name;

	@Schema(description = "檔案描述")
	private String description;

	@Schema(description = "排序值")
	private Integer sort;

	@Schema(description = "儲存地址")
	private String path;
}
