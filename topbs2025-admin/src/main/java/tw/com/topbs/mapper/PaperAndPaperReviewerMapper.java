package tw.com.topbs.mapper;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;

/**
 * <p>
 * 投稿-審稿委員 關聯表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PaperAndPaperReviewerMapper extends BaseMapper<PaperAndPaperReviewer> {

	  /**
     * 分頁查詢審稿人分數統計 (聚合查詢)
     * @param page 分頁參數，注意這裡的泛型是 ReviewerScoreStatsVO
     * @param reviewStage 審稿階段 (可選)
     * @return 包含 ReviewerScoreStatsVO 的分頁結果
     */
    IPage<ReviewerScoreStatsVO> getReviewerScoreStatsPage(IPage<ReviewerScoreStatsVO> page,
                                                          @Param("reviewStage") String reviewStage);
	
}
