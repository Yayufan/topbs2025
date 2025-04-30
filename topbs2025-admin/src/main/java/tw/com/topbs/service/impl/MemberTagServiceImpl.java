package tw.com.topbs.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import tw.com.topbs.mapper.MemberTagMapper;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.service.MemberTagService;

/**
 * <p>
 * member表 和 tag表的關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Service
public class MemberTagServiceImpl extends ServiceImpl<MemberTagMapper, MemberTag> implements MemberTagService {

	@Override
	public List<MemberTag> getMemberTagByTagId(Long tagId) {
		LambdaQueryWrapper<MemberTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(MemberTag::getTagId, tagId);
		List<MemberTag> memberTagList = baseMapper.selectList(currentQueryWrapper);

		return memberTagList;
	}

	@Override
	public void addMemberTag(MemberTag memberTag) {
		baseMapper.insert(memberTag);
	}

	@Override
	public void removeTagRelationsForMembers(Long tagId, Set<Long> membersToRemove) {
		LambdaQueryWrapper<MemberTag> deleteMemberTagWrapper = new LambdaQueryWrapper<>();
		deleteMemberTagWrapper.eq(MemberTag::getTagId, tagId).in(MemberTag::getMemberId, membersToRemove);
		baseMapper.delete(deleteMemberTagWrapper);
	}

}
