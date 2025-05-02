package tw.com.topbs.system.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkResponseVO {

	@Schema(description = "該檔案已接收的chunk數量")
	private Integer uploadedChunkCount;
	
	@Schema(description = "該檔案全部的chunk數量")
	private Integer totalChunks;
	
	@Schema(description = "當前chunk的索引")
	private Integer currentChunkIndex;
	
	@Schema(description = "當前檔案的SHA256值")
	private String currentFileSha256;
	
	@Schema(description = "合併檔案後的儲存路徑,不包含bucketName")
	private String filePath;

}
