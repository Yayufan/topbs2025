package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPublishFileDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPublishFileDTO;
import tw.com.topbs.pojo.entity.PublishFile;

@Mapper(componentModel = "spring")
public interface PublishFileConvert {

	PublishFile addDTOToEntity(AddPublishFileDTO addPublishFileDTO);

	PublishFile putDTOToEntity(PutPublishFileDTO putPublishFileDTO);

}
