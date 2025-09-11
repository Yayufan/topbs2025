package tw.com.topbs.strategy.tag;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.service.MemberTagService;

@Component
@RequiredArgsConstructor
public class MemberTagStrategy implements TagStrategy {

	private final MemberTagService memberTagService;

	@Override
	public String supportType() {
		return "member";
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

}
