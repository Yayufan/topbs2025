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

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.NotificationService;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperReviewerTagService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;

@Component
@RequiredArgsConstructor
public class PaperReviewManager {

	private final String PAPER_REVIEWER_TYPE = "paper-reviewer";
	private final String PAPER_REVIEWER_PREFIX = "R";
	private final int GROUP_SIZE = 200;

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
		Set<Long> currentPaperReviewerIdSet = currentPapersAndReviewers.stream()
				.map(PaperAndPaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 3.業務上在 第X階段審核A稿件 審稿委員只會出現一次
		// 第一階段Z委員審核A稿件，這種關係不會出現兩次
		// 為了後續根據 reviewerId 獲取到 PaperAndPaperReviewer關聯 進行精準刪除，
		Map<Long, PaperAndPaperReviewer> paperAndReviewersMapByReviewerId = currentPapersAndReviewers.stream()
				.collect(Collectors.toMap(PaperAndPaperReviewer::getPaperReviewerId, Function.identity()));

		// 4.根據type 和 姓名，找到一階段 和 二階段標籤列表，有空再優化
		List<Tag> tagList = tagService.getTagByTypeAndFuzzyName(PAPER_REVIEWER_TYPE, PAPER_REVIEWER_PREFIX);

		// reviewStage 是傳入的 String 參數，首先將其轉換為枚舉
		ReviewStageEnum currentStageEnum = ReviewStageEnum.fromValue(reviewStage);

		// 這邊獲取到 R1 Tag 或 R2 Tag 的標籤Ids,假設是R1,會有R1-group-01、R1-group-02、R1-group-03 , 根據總數量而定
		List<Long> tagIds = tagList.stream()
				.filter(tag -> tag.getName().contains(currentStageEnum.getTagPrefix())) // 直接使用從枚舉獲取的前綴進行判斷
				.map(Tag::getTagId)
				.collect(Collectors.toList());

		// ------------------- 移除操作 ------------------------------

		// 5.拿到該移除的集合 和 該新增的集合
		Set<Long> paperReviewersToRemove = Sets.difference(currentPaperReviewerIdSet,
				new HashSet<>(targetPaperReviewerIdList));
		Set<Long> paperReviewersToAdd = Sets.difference(new HashSet<>(targetPaperReviewerIdList),
				currentPaperReviewerIdSet);

		// 6. 要移除的reviewerIds不為空，執行刪除操作
		if (!paperReviewersToRemove.isEmpty()) {

			// 第一步：獲得審稿關係，並刪除
			paperAndPaperReviewerService.batchDeletePapersAndReviewers(paperAndReviewersMapByReviewerId,
					paperReviewersToRemove);

			// 第二步：判斷並刪除 PaperReviewerTag (審稿人資格標籤) 
			// 從這些被移除審稿關係的審稿委員 和 當前審核階段，去查詢是否還有其他 相同階段、不同稿件的情況
			List<Long> reviewerTagsToRemove = paperAndPaperReviewerService
					.getReviewerTagsToRemove(paperAndReviewersMapByReviewerId, paperReviewersToRemove);

			// 第三步：當該被移除的審稿委員ID不為空 以及 tags不為空 ，批量刪除這些審稿人的 Tag 關聯
			if (!reviewerTagsToRemove.isEmpty() && !tagIds.isEmpty()) {
				paperReviewerTagService.deleteByReviewerIdsAndTagIds(currentPaperReviewerIdSet, tagIds);
			}

		}

		// 7. 如果要新增的審稿人ID不為空，開始進行新增操作
		if (!paperReviewersToAdd.isEmpty()) {

			// 第一步：獲取以reviewerId為key , 以PaperReviewer為value的映射對象
			Map<Long, PaperReviewer> reviewerMapById = paperReviewerService.getReviewerMapById(paperReviewersToAdd);

			// 第二步：批量新增 PaperAndPaperReviewer 關係
			paperAndPaperReviewerService.addReviewerToPaper(paperId, reviewStage, reviewerMapById, paperReviewersToAdd);

			// 第二步：判斷並批量新增 PaperReviewerTag (審稿人資格標籤)
			// 找出這些需要新增的審稿人中，哪些在當前 reviewStage 下 還沒有 對應的 Tag
			int groupIndex = paperReviewerService.getReviewerGroupIndex(GROUP_SIZE);
			Tag groupTag = null;

			if (ReviewStageEnum.FIRST_REVIEW.getValue().equals(reviewStage)) {
				groupTag = tagService.getOrCreateFirstReviewerGroupTag(groupIndex);
			} else if (ReviewStageEnum.SECOND_REVIEW.getValue().equals(reviewStage)) {
				groupTag = tagService.getOrCreateSecondReviewerGroupTag(groupIndex);
			} else {
				throw new RuntimeException("沒有對應的階段，無法創建或獲取分組Tag");
			}

			if (groupTag != null && groupTag.getTagId() != null) {
				// 調用新的批次方法來添加 Tag
				// 這會查詢哪些 reviewerId 已經有了 groupTag.getTagId()，並只為沒有的添加
				paperReviewerTagService.addReviewersToTag(groupTag.getTagId(), paperReviewersToAdd);
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

		// 2.獲取全部的稿件 及 評審
		List<Paper> paperList = paperService.getPapersEfficiently();
		List<PaperReviewer> reviewerList = paperReviewerService.getReviewersEfficiently();

		// 3.如果任一資料為空，就不處理
		if (paperList.isEmpty() || reviewerList.isEmpty()) {
			return;
		}

		// 4.初始化兩個關聯, 之後使用批量新增
		List<PaperAndPaperReviewer> relationList = new ArrayList<>();
		// reviewerTag以Map形式是因為要避免重複Tag
		Map<Pair<Long, Long>, PaperReviewerTag> reviewerTagMap = new HashMap<>();

		// 5.撈取一次總數，避免迴圈內反覆查詢當前數量
		long currentCount = paperReviewerService.getReviewerCount();
		AtomicInteger reviewerCounter = new AtomicInteger((int) currentCount);

		for (Paper paper : paperList) {
			String paperAbsType = paper.getAbsType();
			if (paperAbsType == null || paperAbsType.trim().isEmpty())
				continue;

			for (PaperReviewer reviewer : reviewerList) {
				String absTypeList = reviewer.getAbsTypeList();
				if (absTypeList == null || absTypeList.trim().isEmpty())
					continue;

				// 使用逗號分隔再判斷是否包含 absType
				Set<String> reviewerAbsTypes = Arrays.stream(absTypeList.split(","))
						.map(String::trim)
						.collect(Collectors.toSet());
				if (reviewerAbsTypes.contains(paperAbsType.trim())) {

					// 建立關係 第 X 階段審核關係
					PaperAndPaperReviewer relation = new PaperAndPaperReviewer();
					relation.setPaperId(paper.getPaperId());
					relation.setPaperReviewerId(reviewer.getPaperReviewerId());
					relation.setReviewerEmail(reviewer.getEmail());
					relation.setReviewerName(reviewer.getName());
					relation.setReviewStage(reviewStage);
					relationList.add(relation);

					// 為評審新增 第X階段 審核者的 分組標籤標籤
					int groupIndex = (int) Math.ceil((reviewerCounter.getAndIncrement() / (double) GROUP_SIZE));

					Tag groupTag = new Tag();
					if (ReviewStageEnum.FIRST_REVIEW.getValue().equals(reviewStage)) {
						groupTag = tagService.getOrCreateFirstReviewerGroupTag(groupIndex);
					} else if (ReviewStageEnum.SECOND_REVIEW.getValue().equals(reviewStage)) {
						groupTag = tagService.getOrCreateSecondReviewerGroupTag(groupIndex);
					} else {
						throw new PaperAbstractsException("沒有對應的階段，無法創建Tag");
					}

					// 臨時組裝key
					Pair<Long, Long> key = Pair.of(reviewer.getPaperReviewerId(), groupTag.getTagId());
					// 如果沒有這個key 則再添加
					if (!reviewerTagMap.containsKey(key)) {
						reviewerTagMap.put(key,
								new PaperReviewerTag(reviewer.getPaperReviewerId(), groupTag.getTagId()));
					}

				}
			}
		}

		// 6. 批次插入（批量操作提升效率）
		if (!relationList.isEmpty()) {
			paperAndPaperReviewerService.saveBatch(relationList);
		}
		if (!reviewerTagMap.isEmpty()) {
			paperReviewerTagService.saveBatch(reviewerTagMap.values());
		}
	};

}
