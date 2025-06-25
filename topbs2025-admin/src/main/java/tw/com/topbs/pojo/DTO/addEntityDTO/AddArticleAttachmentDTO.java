package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddArticleAttachmentDTO {

	@Schema(description = "文章ID")
	private Long articleId;
	
    @Schema(description = "檔名")
    private String name;

    @Schema(description = "檔案類型")
    private String type;

	
}
