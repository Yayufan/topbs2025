package tw.com.topbs.mapper;

import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperReviewer;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 稿件評審資料表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PaperReviewerMapper extends BaseMapper<PaperReviewer> {

	@Select("SELECT * FROM paper-reviewer WHERE is_deleted = 0")
	List<PaperReviewer> selectReviewers();
	
}
