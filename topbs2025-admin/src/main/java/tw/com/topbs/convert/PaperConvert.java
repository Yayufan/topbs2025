package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.entity.Paper;

@Mapper(componentModel = "spring")
public interface PaperConvert {

	Paper addDTOToEntity(AddPaperDTO addPaperDTO);

	Paper putDTOToEntity(PutPaperDTO putPaperDTO);
	
	Paper putForAdminDTOToEntity(PutPaperForAdminDTO putPaperForAdminDTO);
	
	PaperVO entityToVO(Paper paper);
	
	ReviewVO entityToReviewVO(Paper paper);
	
	
}
