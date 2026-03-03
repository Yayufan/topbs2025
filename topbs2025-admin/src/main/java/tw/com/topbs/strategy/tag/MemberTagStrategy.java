package tw.com.topbs.strategy.tag;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.TagTypeEnum;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.service.MemberTagService;

@Component
@RequiredArgsConstructor
public class MemberTagStrategy implements TagStrategy {

	private final MemberTagService memberTagService;

	@Override
	public String supportType() {
		return TagTypeEnum.MEMBER.getType();
	}

	@Override
	public long countHoldersByTagId(Long tagId) {
		return memberTagService.lambdaQuery().eq(MemberTag::getTagId, tagId).count();
	}

	@Override
	public long countHoldersByTagIds(Collection<Long> tagIds) {
		// 拿到關聯
		List<MemberTag> list = memberTagService.lambdaQuery().in(MemberTag::getTagId, tagIds).list();
		// 收集唯一的 attendeeId
		Set<Long> uniqueMember = list.stream().map(MemberTag::getMemberId).collect(Collectors.toSet());

		return uniqueMember.size();
	}

	@Override
	public List<Long> getAssociatedIdsByTagId(Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 memberTag
		List<MemberTag> currentMemberTags = memberTagService.getMemberTagByTagId(tagId);
		// 2. stream取出memberIdList
		return currentMemberTags.stream().map(MemberTag::getMemberId).toList();
	}

	@Transactional
	@Override
	public void assignEntitiesToTag(List<Long> entityIdList, Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 member
		List<MemberTag> currentMemberTags = memberTagService.getMemberTagByTagId(tagId);

		// 2. 提取當前關聯的 memberId Set
		Set<Long> currentMemberIdSet = currentMemberTags.stream()
				.map(MemberTag::getMemberId)
				.collect(Collectors.toSet());

		// 3. 獲取目標的 memberId 的Set集合
		Set<Long> targetMemberIdSet = new HashSet<>(entityIdList);

		// 4. 計算差集：當前有但目標沒有 → 需刪除
		Set<Long> membersToRemove = Sets.difference(currentMemberIdSet, targetMemberIdSet);

		// 5. 計算差集：目標有但當前沒有 → 需新增
		Set<Long> membersToAdd = Sets.difference(targetMemberIdSet, currentMemberIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!membersToRemove.isEmpty()) {
			memberTagService.removeMembersFromTag(tagId, membersToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!membersToAdd.isEmpty()) {
			memberTagService.addMembersToTag(tagId, membersToAdd);
		}

	}

}
