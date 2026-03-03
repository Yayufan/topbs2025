package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.MemberTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.pojo.entity.Tag;
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
@RequiredArgsConstructor
public class MemberTagServiceImpl extends ServiceImpl<MemberTagMapper, MemberTag> implements MemberTagService {

	private final TagMapper tagMapper;

	@Override
	public Set<Long> getTagIdsByMemberId(Long memberId) {
		// 1.透過memberId 找到member 與 tag 的關聯
		List<MemberTag> memberTagList = this.getMemberTagByMemberId(memberId);

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

	private Map<Long, List<Tag>> baseGroupTagsByMemberId(Collection<Long> memberIds) {
		// 沒有關聯直接返回空映射
		if (memberIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 1. 查詢所有 memberTag 關聯
		LambdaQueryWrapper<MemberTag> memberTagWrapper = new LambdaQueryWrapper<>();
		memberTagWrapper.in(MemberTag::getMemberId, memberIds);
		List<MemberTag> memberTags = baseMapper.selectList(memberTagWrapper);

		// 沒有關聯直接返回空映射
		if (memberTags.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2. 按 memberId 分組，收集 tagId
		Map<Long, List<Long>> memberIdToTagIds = memberTags.stream()
				.collect(Collectors.groupingBy(MemberTag::getMemberId,
						Collectors.mapping(MemberTag::getTagId, Collectors.toList())));

		// 3. 收集所有 tagId，獲取map中所有value,兩層List(Collection<List<Long>>)要拆開
		Set<Long> allTagIds = memberIdToTagIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());

		// 4. 批量查詢所有 Tag，並組成映射關係tagId:Tag
		Map<Long, Tag> tagMap = tagMapper.selectBatchIds(allTagIds)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		// 5. 構建最終結果：memberId -> List<Tag>
		Map<Long, List<Tag>> result = new HashMap<>();

		memberIdToTagIds.forEach((memberId, tagIds) -> {
			List<Tag> tags = tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList());
			result.put(memberId, tags);
		});

		return result;
	}

	@Override
	public Map<Long, List<Tag>> groupTagsByMemberId(Collection<Long> memberIds) {
		return this.baseGroupTagsByMemberId(memberIds);
	}

	@Override
	public Map<Long, List<Tag>> groupTagsByMemberId(List<Member> members) {
		Set<Long> memberIds = members.stream().map(Member::getMemberId).collect(Collectors.toSet());
		return this.baseGroupTagsByMemberId(memberIds);
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
	public void addMembersToTag(Long tagId, Collection<Long> membersToAdd) {
		// 1.建立多個新連結
		List<MemberTag> newMemberTags = membersToAdd.stream().map(memberId -> {
			MemberTag memberTag = new MemberTag();
			memberTag.setTagId(tagId);
			memberTag.setMemberId(memberId);
			return memberTag;
		}).collect(Collectors.toList());

		// 2.批量新增
		this.saveBatch(newMemberTags);

	}

	@Override
	public void removeTagsFromMember(Long memberId, Collection<Long> tagsToRemove) {
		LambdaQueryWrapper<MemberTag> deleteMemberTagWrapper = new LambdaQueryWrapper<>();
		deleteMemberTagWrapper.eq(MemberTag::getMemberId, memberId).in(MemberTag::getTagId, tagsToRemove);
		baseMapper.delete(deleteMemberTagWrapper);

	}

	@Override
	public void removeMembersFromTag(Long tagId, Collection<Long> membersToRemove) {
		LambdaQueryWrapper<MemberTag> deleteMemberTagWrapper = new LambdaQueryWrapper<>();
		deleteMemberTagWrapper.eq(MemberTag::getTagId, tagId).in(MemberTag::getMemberId, membersToRemove);
		baseMapper.delete(deleteMemberTagWrapper);
	}

}
