package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutTagDTO;
import tw.com.topbs.pojo.entity.Tag;

@Mapper(componentModel = "spring")
public interface TagConvert {

	Tag insertDTOToEntity(AddTagDTO addTagDTO);
	
	Tag updateDTOToEntity(PutTagDTO updateTagDTO);
	
}
