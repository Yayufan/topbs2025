package tw.com.topbs.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FormResponseVO {

    @Schema(description = "主鍵ID")
    private Long formResponseId;

    @Schema(description = "表單ID")
    private Long formId;

    @Schema(description = "會員ID , 不是必填 , require_login 為 1 時會有值")
    private Long memberId;
    
    
    
	
}
