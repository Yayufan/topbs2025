package tw.com.topbs.strategy.tag;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.service.PaperTagService;

@Component
@RequiredArgsConstructor
public class PaperTagStrategy implements TagStrategy {

	private final PaperTagService paperTagService;

	@Override
	public String supportType() {
		return "paper";
	}

	@Override
	public long countHoldersByTagId(Long tagId) {
		return paperTagService.lambdaQuery().eq(PaperTag::getTagId, tagId).count();
	}

	@Override
	public long countHoldersByTagIds(Collection<Long> tagIds) {
		// 拿到關聯
		List<PaperTag> list = paperTagService.lambdaQuery().in(PaperTag::getTagId, tagIds).list();
		// 收集唯一的 attendeeId
		Set<Long> uniquePaper = list.stream().map(PaperTag::getPaperId).collect(Collectors.toSet());

		return uniquePaper.size();
	}

}
