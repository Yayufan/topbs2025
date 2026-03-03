package tw.com.topbs.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.enums.TagTypeEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.helper.TagAssignmentHelper;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperReviewerTagService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.TagService;

@Component
@RequiredArgsConstructor
public class PaperReviewManager {

	@Value("${project.group-size}")
	private int GROUP_SIZE;

	private final TagAssignmentHelper tagAssignmentHelper;
	private final PaperService paperService;
	private final TagService tagService;
	private final PaperReviewerService paperReviewerService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;
	private final PaperReviewerTagService paperReviewerTagService;

	/**
	 * 為用戶新增/更新/刪除 複數審稿委員
	 * 
	 * @param reviewStage               審核階段
	 * @param targetPaperReviewerIdList
	 * @param paperId
	 */
	public void assignPaperReviewerToPaper(String reviewStage, List<Long> targetPaperReviewerIdList, Long paperId) {

		// 1. 查詢當前 paper 在指定審核階段的所有關聯 paperReviewer
		List<PaperAndPaperReviewer> currentPapersAndReviewers = paperAndPaperReviewerService
				.getPapersAndReviewersByPaperIdAndReviewStage(paperId, reviewStage);

		// 2. 提取當前關聯的 paperReviewerId Set
		// 這裡只需要獲取與當前 paperId 相關的所有 paperReviewerId，並放入 Set 中以方便比較。
		Set<Long> currentReviewerIdSet = currentPapersAndReviewers.stream()
				.map(PaperAndPaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 3.業務上在 第X階段審核A稿件 審稿委員只會出現一次
		// 第一階段Z委員審核A稿件，這種關係不會出現兩次
		// 為了後續根據 reviewerId 獲取到 PaperAndPaperReviewer關聯 進行精準刪除
		Map<Long, PaperAndPaperReviewer> paperAndReviewersMapByReviewerId = currentPapersAndReviewers.stream()
				.collect(Collectors.toMap(PaperAndPaperReviewer::getPaperReviewerId, Function.identity()));

		// 4.獲取當前審稿狀態
		ReviewStageEnum reviewStageEnum = ReviewStageEnum.fromValue(reviewStage);

		// ------------------- 移除操作 ------------------------------

		// 5.拿到該移除的集合 和 該新增的集合
		Set<Long> reviewersToRemove = Sets.difference(currentReviewerIdSet, new HashSet<>(targetPaperReviewerIdList));
		Set<Long> reviewersToAdd = Sets.difference(new HashSet<>(targetPaperReviewerIdList), currentReviewerIdSet);

		// 6. 要移除的reviewerIds不為空，執行刪除操作
		if (!reviewersToRemove.isEmpty()) {

			// 6-1 獲得審稿關係，並刪除
			paperAndPaperReviewerService.batchDeletePapersAndReviewers(paperAndReviewersMapByReviewerId,
					reviewersToRemove);

			// 6-2 批量查哪些 reviewer 還有其他階段的審稿任務
			Set<Long> stillAssignedReviewers = paperAndPaperReviewerService
					.getReviewerIdsStillAssignedInStage(reviewStage, reviewersToRemove);

			// 6-3 篩選掉仍有審核狀態的審稿者 , 他們不需要移除Tag
			Set<Long> reviewersToRemoveTags = reviewersToRemove.stream()
					.filter(id -> !stillAssignedReviewers.contains(id))
					.collect(Collectors.toSet());

			// 6-4 如果有需要移除Tag的Reviewers再執行刪除Tag
			if (!reviewersToRemoveTags.isEmpty()) {

				// 6-4-1 批量移除 第X階段 審稿人 Tag
				tagAssignmentHelper.batchRemoveGroupTagsByPattern(reviewersToRemoveTags,
						TagTypeEnum.PAPER_REVIEWER.getType(), reviewStageEnum.getTagPrefix(),
						tagService::getTagIdsByTypeAndNamePattern, paperReviewerTagService::removeTagsFromReviewers);

				// 6-4-2 批量移除 第X階段 審稿人-未審核完畢 Tag
				tagAssignmentHelper.batchRemoveGroupTagsByPattern(reviewersToRemoveTags,
						TagTypeEnum.PAPER_REVIEWER.getType(), reviewStageEnum.getNotReviewTagPrefix(),
						tagService::getTagIdsByTypeAndNamePattern, paperReviewerTagService::removeTagsFromReviewers);

			}

		}

		// 7. 如果要新增的審稿人ID不為空，開始進行新增操作
		if (!reviewersToAdd.isEmpty()) {

			// 第一步：獲取以reviewerId為key , 以PaperReviewer為value的映射對象
			Map<Long, PaperReviewer> reviewerMapById = paperReviewerService.getReviewerMapById(reviewersToAdd);

			// 第二步：批量新增 PaperAndPaperReviewer 關係
			paperAndPaperReviewerService.addReviewerToPaper(paperId, reviewStage, reviewerMapById, reviewersToAdd);

			// 第三步：判斷並批量新增 PaperReviewerTag (審稿人資格標籤)
			// 拿到該階段審稿人的總數,這時已經是DB中有新增關聯的實際數量了
			long reviewerCount = paperAndPaperReviewerService.getReviewerCountByReviewStage(reviewStage);
			int groupIndex = (int) Math.ceil(reviewerCount / GROUP_SIZE);

			// 第四步：根據審核階段,給予不同的tag,目前新增tag會有額外tag產生的情況
			switch (reviewStageEnum) {
			case FIRST_REVIEW -> {
				// 第一輪審核者 Tag
				tagAssignmentHelper.batchAssignTagWithIndex(reviewersToAdd, groupIndex,
						tagService::getOrCreateStage1ReviewerGroupTag,
						paperReviewerTagService::addUniqueReviewersToTag);

				// 第一輪審核者 未審核完畢 Tag
				tagAssignmentHelper.batchAssignTagWithIndex(reviewersToAdd, groupIndex,
						tagService::getOrCreateNotReviewInStage1GroupTag,
						paperReviewerTagService::addUniqueReviewersToTag);

			}
			case SECOND_REVIEW -> {
				// 第二輪審核者 Tag
				tagAssignmentHelper.batchAssignTagWithIndex(reviewersToAdd, groupIndex,
						tagService::getOrCreateStage2ReviewerGroupTag,
						paperReviewerTagService::addUniqueReviewersToTag);

				// 第二輪審核者 未審核 Tag
				tagAssignmentHelper.batchAssignTagWithIndex(reviewersToAdd, groupIndex,
						tagService::getOrCreateNotReviewInStage2GroupTag,
						paperReviewerTagService::addUniqueReviewersToTag);

			}
			default -> {
				throw new RuntimeException("沒有對應的階段，無法創建或獲取分組Tag");
			}
			}

		}

	};

	/**
	 * 只要審稿委員符合稿件類型，且沒有相同審核階段的記錄，就自動進行分配
	 * 
	 * @param reviewStage
	 */
	public void autoAssignPaperReviewer(String reviewStage) {

		// 1.拿到當前的關聯狀態,如果有任何一筆就不能使用自動分配
		long count = paperAndPaperReviewerService.getPaperReviewersByReviewStage(reviewStage);
		if (count > 0) {
			throw new PaperAbstractsException("已存在分配記錄，無法自動分配");
		}

		// 2.獲取當前審稿狀態
		ReviewStageEnum reviewStageEnum = ReviewStageEnum.fromValue(reviewStage);

		// 3.獲取全部的稿件 及 評審
		List<Paper> paperList = paperService.getPapersEfficiently();
		List<PaperReviewer> reviewerList = paperReviewerService.getReviewersEfficiently();

		// 4.如果任一資料為空，就不處理
		if (paperList.isEmpty() || reviewerList.isEmpty()) {
			return;
		}

		// 5.初始化兩個關聯, 之後使用批量新增
		List<PaperAndPaperReviewer> relationList = new ArrayList<>();
		
		// 6.reviewerTag以Map形式是因為要避免重複Tag
		Map<Pair<Long, Long>, PaperReviewerTag> reviewerTagMap = new HashMap<>();

		// 7.預先整理 reviewer 的 absTypeSet
		Map<Long, Set<String>> reviewerAbsTypeMap = reviewerList.stream()
				.filter(r -> r.getAbsTypeList() != null)
				.collect(Collectors.toMap(PaperReviewer::getPaperReviewerId,
						r -> Arrays.stream(r.getAbsTypeList().split(","))
								.map(String::trim)
								.collect(Collectors.toSet())));

		// 8.預先整理 paper 的 分組type桶 , 加速效率
		Map<String, List<Paper>> paperByType = paperList.stream().collect(Collectors.groupingBy(Paper::getAbsType));

		// 9.預先整理可能會發配的Tag
		int maxGroupIndex = (int) Math.ceil(reviewerList.size() / (double) GROUP_SIZE);
		// 預先儲存第一/二階段審稿關係會用到的Tag
		Map<Integer, List<Tag>> stage1TagCache = new HashMap<>();
		Map<Integer, List<Tag>> stage2TagCache = new HashMap<>();
		for (int i = 1; i <= maxGroupIndex; i++) {
			stage1TagCache.put(i, List.of(tagService.getOrCreateStage1ReviewerGroupTag(i),
					tagService.getOrCreateNotReviewInStage1GroupTag(i)));

			stage2TagCache.put(i, List.of(tagService.getOrCreateStage2ReviewerGroupTag(i),
					tagService.getOrCreateNotReviewInStage2GroupTag(i)));
		}

		// 10.reviewer計數從1開始計算
		AtomicInteger reviewerCounter = new AtomicInteger(1);

		// 11.開始遍歷 reviewer 和 paper 達成業務上的N*M 把所有稿件發配給合適的審稿人
		for (PaperReviewer reviewer : reviewerList) {

			// 11-1 拿到此審稿人可審核的稿件類別,沒有則跳過
			Set<String> absTypes = reviewerAbsTypeMap.get(reviewer.getPaperReviewerId());
			if (absTypes == null || absTypes.isEmpty())
				continue;

			// 11-2 拿到 審核者 的分組標籤標籤
			int groupIndex = (int) Math.ceil((reviewerCounter.getAndIncrement() / (double) GROUP_SIZE));

			for (String type : absTypes) {
				// 取出該 reviewer 能審查的所有 paper（不是全部 paper）
				// 符合 腫瘤 類別的稿件
				List<Paper> targetPapers = paperByType.get(type);
				if (targetPapers == null)
					continue;

				for (Paper paper : targetPapers) {
					// 如果該篇稿件的類別,符合審稿人可審類別

					// 建立關係 第 X 階段審核關係
					PaperAndPaperReviewer relation = new PaperAndPaperReviewer();
					relation.setPaperId(paper.getPaperId());
					relation.setPaperReviewerId(reviewer.getPaperReviewerId());
					relation.setReviewerEmail(reviewer.getEmail());
					relation.setReviewerName(reviewer.getName());
					relation.setReviewStage(reviewStage);
					relationList.add(relation);

					// 根據審核階段獲取對應的兩個Tag
					List<Tag> tags = switch (reviewStageEnum) {
					case FIRST_REVIEW -> stage1TagCache.get(groupIndex);
					case SECOND_REVIEW -> stage2TagCache.get(groupIndex);
					default -> throw new RuntimeException("沒有對應的階段，無法創建或獲取分組Tag");
					};

					// 批量添加Tag關聯
					Long reviewerId = reviewer.getPaperReviewerId();
					for (Tag tag : tags) {
						Pair<Long, Long> key = Pair.of(reviewerId, tag.getTagId());
						reviewerTagMap.putIfAbsent(key, new PaperReviewerTag(reviewerId, tag.getTagId()));
					}

				}
			}

		}

		// 12. 批次插入（批量操作提升效率）
		if (!relationList.isEmpty()) {
			paperAndPaperReviewerService.saveBatch(relationList);
		}
		if (!reviewerTagMap.isEmpty()) {
			paperReviewerTagService.saveBatch(reviewerTagMap.values());
		}
	};

}
