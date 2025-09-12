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

	@Override
	public List<Long> getAssociatedIdsByTagId(Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 paperTag
		List<PaperTag> paperTagList = paperTagService.getPaperTagByTagId(tagId);
		// 2. stream取出 paperIdList
		List<Long> paperIdList = paperTagList.stream().map(PaperTag::getPaperId).collect(Collectors.toList());
		return paperIdList;
	}

	@Transactional
	@Override
	public void assignEntitiesToTag(List<Long> entityIdList, Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 paper
		List<PaperTag> currentPaperTags = paperTagService.getPaperTagByTagId(tagId);

		// 2. 提取當前關聯的 paperId Set
		Set<Long> currentPaperIdSet = currentPaperTags.stream().map(PaperTag::getPaperId).collect(Collectors.toSet());

		// 3. 對比目標 paperIdList 和當前 paperIdList
		Set<Long> targetPaperIdSet = new HashSet<>(entityIdList);

		// 4. 計算差集：當前有但目標沒有 → 需刪除
		Set<Long> papersToRemove = Sets.difference(currentPaperIdSet, targetPaperIdSet);

		// 5. 計算差集：目標有但當前沒有 → 需新增
		Set<Long> papersToAdd = Sets.difference(targetPaperIdSet, currentPaperIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!papersToRemove.isEmpty()) {
			paperTagService.removePapersFromTag(tagId, papersToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!papersToAdd.isEmpty()) {

			List<PaperTag> newPaperTags = papersToAdd.stream().map(paperId -> {
				PaperTag paperTag = new PaperTag();
				paperTag.setTagId(tagId);
				paperTag.setPaperId(paperId);
				return paperTag;
			}).collect(Collectors.toList());

			// 批量插入
			paperTagService.saveBatch(newPaperTags);
		}

	}

}
