package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;

@Mapper(componentModel = "spring")
public interface PaperConvert {

	Paper addDTOToEntity(AddPaperDTO addPaperDTO);

	Paper putDTOToEntity(PutPaperDTO putPaperDTO);
	
	PaperVO entityToVO(Paper paper);
	
	List<PaperVO> entityListToVOList(List<Paper> paperList);
	
}
