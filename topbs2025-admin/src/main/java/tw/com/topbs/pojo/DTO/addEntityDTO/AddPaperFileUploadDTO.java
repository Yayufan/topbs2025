package tw.com.topbs.pojo.DTO.addEntityDTO;

import com.baomidou.mybatisplus.annotation.TableField;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AddPaperFileUploadDTO {

	@Schema(description = "摘要ID")
	private Long paperId;
	
	@Schema(description = "分類成 abstract_pdf, abstract_docx, abstract_slide, offical_document; 用來接收 投稿PDF、投稿WORD、投稿PPT、公文檔案")
	@TableField("type")
	private String type;

	@Schema(description = "檔案名稱-可與傳送時不同")
	@TableField("file_name")
	private String fileName;

	@Schema(description = "檔案在minio儲存的路徑")
	@TableField("path")
	private String path;

}
