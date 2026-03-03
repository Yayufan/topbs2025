package tw.com.topbs.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.pojo.entity.Paper;

/**
 * <p>
 * 投稿資料表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface PaperMapper extends BaseMapper<Paper> {

	@Select("SELECT * FROM paper WHERE is_deleted = 0")
	List<Paper> selectPapers();
	
}
