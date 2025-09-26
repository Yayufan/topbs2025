package tw.com.topbs.pojo.DTO;

import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;

@Data
public class TestAddPaperDTO {

	@Schema(description = "稿件資訊")
	AddPaperDTO addPaperDTO;

	@Schema(description = "稿件附件")
	MultipartFile[] files;

}
