package tw.com.topbs.pojo.DTO.addEntityDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddEmailTemplateDTO {
	
	@NotBlank
    @Schema(description = "信件模板名稱")
    private String name;
    
    @Schema(description = "信件模板描述")
    private String description;
}
