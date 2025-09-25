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
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.service.AttendeesTagService;

@Component
@RequiredArgsConstructor
public class AttendeesTagStrategy implements TagStrategy {

	private final AttendeesTagService attendeesTagService;

	@Override
	public String supportType() {
		return TagTypeEnum.ATTENDEES.getType();
	}

	@Override
	public long countHoldersByTagId(Long tagId) {
		return attendeesTagService.lambdaQuery().eq(AttendeesTag::getTagId, tagId).count();
	}

	@Override
	public long countHoldersByTagIds(Collection<Long> tagIds) {
		// 拿到關聯
		List<AttendeesTag> list = attendeesTagService.lambdaQuery().in(AttendeesTag::getTagId, tagIds).list();
		// 收集唯一的 attendeeId
		Set<Long> uniqueAttendees = list.stream().map(AttendeesTag::getAttendeesId).collect(Collectors.toSet());

		return uniqueAttendees.size();
	}

	@Override
	public List<Long> getAssociatedIdsByTagId(Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 attendeesTag
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByTagId(tagId);
		// 2. stream取出 attendeesIdList
		return attendeesTagList.stream().map(AttendeesTag::getAttendeesId).toList();
	}

	@Transactional
	@Override
	public void assignEntitiesToTag(List<Long> entityIdList, Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 attendees
		List<AttendeesTag> currentAttendeesTags = attendeesTagService.getAttendeesTagByTagId(tagId);

		// 2. 提取當前關聯的 attendeesId Set
		Set<Long> currentAttendeesIdSet = currentAttendeesTags.stream()
				.map(AttendeesTag::getAttendeesId)
				.collect(Collectors.toSet());

		// 3. 獲取目標的 attendeesId 的Set集合
		Set<Long> targetAttendeesIdSet = new HashSet<>(entityIdList);

		// 4. 計算差集：當前有但目標沒有 → 需刪除
		Set<Long> attendeesToRemove = Sets.difference(currentAttendeesIdSet, targetAttendeesIdSet);

		// 5. 計算差集：目標有但當前沒有 → 需新增
		Set<Long> attendeesToAdd = Sets.difference(targetAttendeesIdSet, currentAttendeesIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!attendeesToRemove.isEmpty()) {
			attendeesTagService.removeAttendeesFromTag(tagId, attendeesToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!attendeesToAdd.isEmpty()) {
			attendeesTagService.addAttendeesToTag(tagId, attendeesToAdd);
		}

	}

}
