package tw.com.topbs.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.com.topbs.enums.PaperStatusEnum;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperTagVO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.excelPojo.PaperScoreExcel;

@Mapper(componentModel = "spring")
public interface PaperConvert {

	Paper addDTOToEntity(AddPaperDTO addPaperDTO);

	Paper putDTOToEntity(PutPaperDTO putPaperDTO);

	Paper putForAdminDTOToEntity(PutPaperForAdminDTO putPaperForAdminDTO);


	/**
	 * 給投稿者的VO
	 * 
	 * @param paper
	 * @return
	 */
	PaperVO entityToVO(Paper paper);
	
	/**
	 * 給管理者的詳細VO
	 * 
	 * @param paper
	 * @return
	 */
	PaperTagVO entityToTagVO(Paper paper);

	ReviewVO entityToReviewVO(Paper paper);

	@Mapping(source = "paperId", target = "paperId", qualifiedByName = "convertLongToString")
	@Mapping(source = "memberId", target = "memberId", qualifiedByName = "convertLongToString")
	@Mapping(source = "status", target = "status", qualifiedByName = "convertStatusToString")
	PaperScoreExcel entityToExcel(Paper paper);

	@Named("convertLongToString")
	default String convertLongToString(Long id) {
		return id.toString();
	}

	@Named("convertStatusToString")
	default String convertStatusToString(Integer status) {
		return PaperStatusEnum.fromValue(status).getLabelZh();

	}

}
