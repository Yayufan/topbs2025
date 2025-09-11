package tw.com.topbs.strategy.tag;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.service.AttendeesTagService;

@Component
@RequiredArgsConstructor
public class AttendeesTagStrategy implements TagStrategy {

	private final AttendeesTagService attendeesTagService;

	@Override
	public String supportType() {
		return "attendees";
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

}
