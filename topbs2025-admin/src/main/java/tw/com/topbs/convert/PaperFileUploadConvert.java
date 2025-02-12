package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
import tw.com.topbs.pojo.VO.PaperFileUploadVO;
import tw.com.topbs.pojo.entity.PaperFileUpload;

@Mapper(componentModel = "spring")
public interface PaperFileUploadConvert {

	PaperFileUpload addDTOToEntity(AddPaperFileUploadDTO addPaperFileUploadDTO);

	PaperFileUpload putDTOToEntity(PutPaperFileUploadDTO putPaperFileUploadDTO);
	
	PaperFileUploadVO entityToVO(PaperFileUpload paperFileUpload);
	
	List<PaperFileUploadVO> entityListToVOList(List<PaperFileUpload> paperFileUploadList);
	
}
