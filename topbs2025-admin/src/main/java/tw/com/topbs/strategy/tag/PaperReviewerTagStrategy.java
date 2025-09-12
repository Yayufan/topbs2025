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
		List<PaperReviewerTag> list = paperReviewerTagService.lambdaQuery()
				.in(PaperReviewerTag::getTagId, tagIds)
				.list();
		// 收集唯一的 attendeeId
		Set<Long> uniquePaperReviewer = list.stream()
				.map(PaperReviewerTag::getPaperReviewerId)
				.collect(Collectors.toSet());

		return uniquePaperReviewer.size();
	}

	@Override
	public List<Long> getAssociatedIdsByTagId(Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 paperReviewer
		List<PaperReviewerTag> paperReviewerTagList = paperReviewerTagService.getPaperReviewerTagByTagId(tagId);
		// 2. stream取出 paperReviewerIdList
		List<Long> paperReviewerIdList = paperReviewerTagList.stream()
				.map(PaperReviewerTag::getPaperReviewerId)
				.collect(Collectors.toList());
		return paperReviewerIdList;
	}

	@Transactional
	@Override
	public void assignEntitiesToTag(List<Long> entityIdList, Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 paperReviewer
		List<PaperReviewerTag> currentPaperReviewerTags = paperReviewerTagService.getPaperReviewerTagByTagId(tagId);

		// 2. 提取當前關聯的 paperReviewerId Set
		Set<Long> currentPaperReviewerIdSet = currentPaperReviewerTags.stream()
				.map(PaperReviewerTag::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 3. 對比目標 paperReviewerIdList 和當前 paperReviewerIdList
		Set<Long> targetPaperReviewerIdSet = new HashSet<>(entityIdList);

		// 4. 計算差集：當前有但目標沒有 → 需刪除
		Set<Long> paperReviewersToRemove = Sets.difference(currentPaperReviewerIdSet, targetPaperReviewerIdSet);

		// 5. 計算差集：目標有但當前沒有 → 需新增
		Set<Long> paperReviewersToAdd = Sets.difference(targetPaperReviewerIdSet, currentPaperReviewerIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!paperReviewersToRemove.isEmpty()) {
			paperReviewerTagService.removePaperReviewerFromTag(tagId, paperReviewersToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!paperReviewersToAdd.isEmpty()) {
			List<PaperReviewerTag> newPaperReviewerTags = paperReviewersToAdd.stream().map(paperReviewerId -> {
				PaperReviewerTag paperReviewerTag = new PaperReviewerTag();
				paperReviewerTag.setTagId(tagId);
				paperReviewerTag.setPaperReviewerId(paperReviewerId);
				return paperReviewerTag;
			}).collect(Collectors.toList());

			// 批量插入
			paperReviewerTagService.saveBatch(newPaperReviewerTags);
		}

	}

}
