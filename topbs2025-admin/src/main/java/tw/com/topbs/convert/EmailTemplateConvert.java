package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.com.topbs.pojo.entity.EmailTemplate;

@Mapper(componentModel = "spring")
public interface EmailTemplateConvert {

	EmailTemplate insertDTOToEntity(AddEmailTemplateDTO addArticleDTO);

	EmailTemplate updateDTOToEntity(PutEmailTemplateDTO updateArticleDTO);
	
}
