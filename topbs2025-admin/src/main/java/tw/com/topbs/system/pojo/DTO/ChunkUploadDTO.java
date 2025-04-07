package tw.com.topbs.system.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChunkUploadDTO {

	@Schema(description = "文件唯一標示，可由前端或後端產生都可以，一致就好")
	private String fileId;

	@NotBlank
	@Schema(description = "文件MD5值，用於秒傳")
	private String fileMd5;

	@NotNull
	@Schema(description = "當前分片索引")
	private Integer chunkIndex;

	@NotNull
	@Schema(description = "所有分片總數")
	private Integer totalChunks;

}
