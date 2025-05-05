package tw.com.topbs.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.pojo.entity.Member;

/**
 * <p>
 * 會員表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface MemberMapper extends BaseMapper<Member> {

	@Select("SELECT * FROM member WHERE is_deleted = 0")
	List<Member> selectMembers();
}
