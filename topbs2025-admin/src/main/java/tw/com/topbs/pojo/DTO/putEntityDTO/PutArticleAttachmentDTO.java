package tw.com.topbs.pojo.DTO.putEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PutArticleAttachmentDTO {

    @Schema(description = "文章附件主鍵ID")
    private Long articleAttachmentId;
    

    @Schema(description = "檔名")
    private String name;

    @Schema(description = "檔案類型")
    private String type;

    @Schema(description = "儲存地址")
    private String path;
    

}
