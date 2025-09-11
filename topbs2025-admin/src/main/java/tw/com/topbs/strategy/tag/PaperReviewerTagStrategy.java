package tw.com.topbs.strategy.tag;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.service.PaperReviewerTagService;

@Component
@RequiredArgsConstructor
public class PaperReviewerTagStrategy implements TagStrategy {

	private final PaperReviewerTagService paperReviewerTagService;

	@Override
	public String supportType() {
		return "paper-reviewer";
	}

	@Override
	public long countHoldersByTagId(Long tagId) {
		return paperReviewerTagService.lambdaQuery().eq(PaperReviewerTag::getTagId, tagId).count();
	}

	@Override
	public long countHoldersByTagIds(Collection<Long> tagIds) {
		// 拿到關聯
		List<PaperReviewerTag> list = paperReviewerTagService.lambdaQuery().in(PaperReviewerTag::getTagId, tagIds).list();
		// 收集唯一的 attendeeId
		Set<Long> uniquePaperReviewer = list.stream().map(PaperReviewerTag::getPaperReviewerId).collect(Collectors.toSet());

		return uniquePaperReviewer.size();
	}

}
