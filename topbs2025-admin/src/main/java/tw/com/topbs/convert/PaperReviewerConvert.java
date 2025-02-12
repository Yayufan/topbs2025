package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.PaperReviewer;

@Mapper(componentModel = "spring")
public interface PaperReviewerConvert {


	PaperReviewer addDTOToEntity(AddPaperReviewerDTO addPaperReviewerDTO);

	PaperReviewer putDTOToEntity(PutPaperReviewerDTO putPaperReviewerDTO);
	
	PaperReviewerVO entityToVO(PaperReviewer paperReviewer);
	
	List<PaperReviewerVO> entityListToVOList(List<PaperReviewer> paperReviewerList);
}
