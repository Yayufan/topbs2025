package tw.com.topbs.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;

@Data
public class SlideUploadDTO {

	@NotNull
	@Schema(description = "稿件ID")
	private Long paperId;

	@Valid
	@NotNull
	@Schema(description = "分片上傳資訊")
	private ChunkUploadDTO chunkUploadDTO;

}
