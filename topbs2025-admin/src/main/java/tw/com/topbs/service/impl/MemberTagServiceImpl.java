package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
	public Set<Long> getTagIdsByMemberId(Long memberId) {
		// 1.透過memberId 找到member 與 tag 的關聯
		LambdaQueryWrapper<MemberTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(MemberTag::getMemberId, memberId);
		List<MemberTag> memberTagList = baseMapper.selectList(currentQueryWrapper);

		// 2.透過stream流抽取tagId, 變成Set集合
		return memberTagList.stream().map(memberTag -> memberTag.getTagId()).collect(Collectors.toSet());

	}

	@Override
	public List<MemberTag> getMemberTagByMemberId(Long memberId) {
		LambdaQueryWrapper<MemberTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(MemberTag::getMemberId, memberId);
		List<MemberTag> memberTagList = baseMapper.selectList(currentQueryWrapper);

		return memberTagList;
	}

	@Override
	public List<MemberTag> getMemberTagByTagId(Long tagId) {
		LambdaQueryWrapper<MemberTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(MemberTag::getTagId, tagId);
		List<MemberTag> memberTagList = baseMapper.selectList(currentQueryWrapper);

		return memberTagList;
	}

	@Override
	public List<MemberTag> getMemberTagByMemberIds(Collection<Long> memberIds) {
		LambdaQueryWrapper<MemberTag> memberTagWrapper = new LambdaQueryWrapper<>();
		memberTagWrapper.in(MemberTag::getMemberId, memberIds);
		List<MemberTag> memberTagList = baseMapper.selectList(memberTagWrapper);
		return memberTagList;
	}

	@Override
	public List<MemberTag> getMemberTagByTagIds(Collection<Long> tagIds) {
		LambdaQueryWrapper<MemberTag> memberTagWrapper = new LambdaQueryWrapper<>();
		memberTagWrapper.in(MemberTag::getTagId, tagIds);
		List<MemberTag> memberTagList = baseMapper.selectList(memberTagWrapper);
		return memberTagList;
	}

	@Override
	public void addMemberTag(MemberTag memberTag) {
		baseMapper.insert(memberTag);
	}

	@Override
	public void addMemberTag(Long memberId, Long tagId) {
		MemberTag memberTag = new MemberTag();
		memberTag.setMemberId(memberId);
		memberTag.setTagId(tagId);
		baseMapper.insert(memberTag);
	}

	@Override
	public void addTagsToMember(Long memberId, Collection<Long> tagsToAdd) {

		// 1.建立多個新連結
		List<MemberTag> newMemberTags = tagsToAdd.stream().map(tagId -> {
			MemberTag memberTag = new MemberTag();
			memberTag.setTagId(tagId);
			memberTag.setMemberId(memberId);
			return memberTag;
		}).collect(Collectors.toList());

		// 2.批量新增
		this.saveBatch(newMemberTags);

	}

	@Override
	public void removeMembersFromTag(Long tagId, Collection<Long> membersToRemove) {
		LambdaQueryWrapper<MemberTag> deleteMemberTagWrapper = new LambdaQueryWrapper<>();
		deleteMemberTagWrapper.eq(MemberTag::getTagId, tagId).in(MemberTag::getMemberId, membersToRemove);
		baseMapper.delete(deleteMemberTagWrapper);
	}

	@Override
	public void removeTagsFromMember(Long memberId, Collection<Long> tagsToRemove) {
		LambdaQueryWrapper<MemberTag> deleteMemberTagWrapper = new LambdaQueryWrapper<>();
		deleteMemberTagWrapper.eq(MemberTag::getMemberId, memberId).in(MemberTag::getTagId, tagsToRemove);
		baseMapper.delete(deleteMemberTagWrapper);

	}

}
