package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;

@Mapper(componentModel = "spring")
public interface PaperAndPaperReviewerConvert {


	PaperAndPaperReviewer putDTOToEntity(PutPaperReviewDTO putPaperReviewDTO);

	AssignedReviewersVO entityToAssignedReviewersVO(PaperAndPaperReviewer paperAndPaperReviewer);

	ReviewerScoreStatsVO entityToReviewerScoreStatsVO(PaperAndPaperReviewer paperAndPaperReviewer);
}
