package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddEmailTemplateDTO {
	
    @Schema(description = "類別，用於拆分Email模板適合的人群")
    private String category;
	
	@NotBlank
    @Schema(description = "信件模板名稱")
    private String name;
    
    @Schema(description = "信件模板描述")
    private String description;
}
