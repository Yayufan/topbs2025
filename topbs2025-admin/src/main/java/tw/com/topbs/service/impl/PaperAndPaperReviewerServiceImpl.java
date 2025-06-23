package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperAndPaperReviewerConvert;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.PaperAbstructsException;
import tw.com.topbs.manager.PaperManager;
import tw.com.topbs.mapper.PaperAndPaperReviewerMapper;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperReviewerTagService;
import tw.com.topbs.service.TagService;

/**
 * <p>
 * 投稿-審稿委員 關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Service
@RequiredArgsConstructor
public class PaperAndPaperReviewerServiceImpl extends ServiceImpl<PaperAndPaperReviewerMapper, PaperAndPaperReviewer>
		implements PaperAndPaperReviewerService {

	private final String PAPER_REVIEWER_TYPE = "paper_reviewer";
	private final String PAPER_REVIEWER_PREFIX = "R";
	private final int GROUP_SIZE = 200;

	private final SqlSessionFactory sqlSessionFactory;
	private final PaperMapper paperMapper;
	private final PaperManager paperManager;
	private final PaperConvert paperConvert;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperReviewerMapper paperReviewerMapper;
	private final PaperReviewerTagService paperReviewerTagService;
	private final PaperAndPaperReviewerConvert paperAndPaperReviewerConvert;
	private final TagService tagService;
	private final TransactionTemplate transactionTemplate;

	@Override
	public Map<Long, List<PaperAndPaperReviewer>> groupPaperReviewersByPaperId(String reviewStage) {
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(StringUtils.isNotBlank(reviewStage), PaperAndPaperReviewer::getReviewStage, reviewStage);
		List<PaperAndPaperReviewer> papersReviewers = baseMapper.selectList(queryWrapper);

		return papersReviewers.stream().collect(Collectors.groupingBy(PaperAndPaperReviewer::getPaperId));

	}

	@Override
	public Map<Long, List<AssignedReviewersVO>> groupPaperReviewersByPaperId(List<Long> paperIds) {

		// 1.如果paperIds為空，返回空Map
		if (paperIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2.查詢符合的 關聯關係
		LambdaQueryWrapper<PaperAndPaperReviewer> papersAndReviewerWrapper = new LambdaQueryWrapper<>();
		papersAndReviewerWrapper.in(PaperAndPaperReviewer::getPaperId, paperIds);
		List<PaperAndPaperReviewer> papersAndReviewers = baseMapper.selectList(papersAndReviewerWrapper);

		// 3.返回paperId為key, assignedReviewersVO 為值的Map
		Map<Long, List<AssignedReviewersVO>> result = papersAndReviewers.stream()
				.map(paperAndPaperReviewerConvert::entityToAssignedReviewersVO) // 轉換成 VO
				.collect(Collectors.groupingBy(AssignedReviewersVO::getPaperId // 按 paperId 分組
				));

		return result;

	}

	@Override
	public List<PaperAndPaperReviewer> getPapersAndReviewersByReviewerId(Long paperReviewerId) {
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getPaperReviewerId, paperReviewerId);
		List<PaperAndPaperReviewer> papersAndReviewers = baseMapper.selectList(queryWrapper);

		return papersAndReviewers;
	}

	@Override
	public IPage<ReviewVO> getReviewVOPageByReviewerIdAtFirstReview(IPage<PaperAndPaperReviewer> pageable,
			Long reviewerId) {

		// 這邊使用方法引用，原本是一個Lambda (paperIds) -> paperFileUploadService.getPaperFileMapByPaperIdAtFirstReviewStage(paperIds)
		return this.getReviewVOPageByReviewerIdAndReviewStage(pageable, reviewerId, ReviewStageEnum.FIRST_REVIEW,
				paperFileUploadService::getPaperFileMapByPaperIdAtFirstReviewStage);
	}

	@Override
	public IPage<ReviewVO> getReviewVOPageByReviewerIdAtSecondReview(IPage<PaperAndPaperReviewer> pageable,
			Long reviewerId) {

		// 這邊使用方法引用，原本是一個Lambda (paperIds) -> paperFileUploadService.getPaperFileMapByPaperIdAtSecondReviewStage(paperIds)
		return this.getReviewVOPageByReviewerIdAndReviewStage(pageable, reviewerId, ReviewStageEnum.SECOND_REVIEW,
				paperFileUploadService::getPaperFileMapByPaperIdAtSecondReviewStage);
	}

	/**
	 * 抽取共同邏輯的私有方法
	 *
	 * @param pageable       分頁資訊
	 * @param reviewerId     審稿人ID
	 * @param reviewStage    審稿階段
	 * @param fileMapFetcher 獲取稿件檔案映射的函數，根據審稿階段不同而異
	 * @return 審稿VO的分頁對象
	 */
	private IPage<ReviewVO> getReviewVOPageByReviewerIdAndReviewStage(IPage<PaperAndPaperReviewer> pageable,
			Long reviewerId, ReviewStageEnum reviewStage,

			// Function<List<Long>, Map<Long, List<PaperFileUpload>>> fileMapFetcher 
			// 表示一個接收 List<Long>（paperIds）並返回 Map<Long, List<PaperFileUpload>> 的函數。
			Function<List<Long>, Map<Long, List<PaperFileUpload>>> fileMapFetcher) {

		// 1.根據paperReviewerId 和 reviewStage查詢應審核稿件
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getPaperReviewerId, reviewerId)
				.eq(PaperAndPaperReviewer::getReviewStage, reviewStage.getValue());
		IPage<PaperAndPaperReviewer> papersAndReviewersPage = baseMapper.selectPage(pageable, queryWrapper);

		// 2.如果沒有數據就直接返回空分頁對象
		if (papersAndReviewersPage.getRecords().isEmpty()) {
			return new Page<>(pageable.getCurrent(), pageable.getSize());
		}

		// 3.獲取到稿件ID, 並以此獲得 稿件的映射對象
		List<Long> paperIds = papersAndReviewersPage.getRecords()
				.stream()
				.map(PaperAndPaperReviewer::getPaperId)
				.collect(Collectors.toList());
		Map<Long, Paper> paperMapById = paperManager.getPaperMapById(paperIds);

		// 4.獲取稿件檔案映射檔案 (透過傳入的函數來決定是第一階段還是第二階段的檔案)
		Map<Long, List<PaperFileUpload>> paperFileMapByPaperId = fileMapFetcher.apply(paperIds);

		// 5.遍歷 papersAndReviewersPage 產生 List<ReviewVO> 對象
		List<ReviewVO> reviewVOList = papersAndReviewersPage.getRecords().stream().map(papersAndReviewers -> {
			ReviewVO reviewVO = paperConvert.entityToReviewVO(paperMapById.get(papersAndReviewers.getPaperId()));
			reviewVO.setFileList(
					paperFileMapByPaperId.getOrDefault(papersAndReviewers.getPaperId(), Collections.emptyList()));
			reviewVO.setPaperAndPaperReviewerId(papersAndReviewers.getPaperAndPaperReviewerId());
			reviewVO.setScore(papersAndReviewers.getScore());
			return reviewVO;
		}).collect(Collectors.toList());

		// 6.創建一個reviewVOPage 分頁對象回傳
		Page<ReviewVO> reviewVOPage = new Page<>(papersAndReviewersPage.getCurrent(), papersAndReviewersPage.getSize(),
				papersAndReviewersPage.getTotal());
		reviewVOPage.setRecords(reviewVOList);

		return reviewVOPage;
	}

	@Override
	public IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable,
			String reviewStage) {

		return baseMapper.getReviewerScoreStatsPage(pageable, reviewStage);

	}

	@Override
	@Transactional
	public void autoAssignPaperReviewer(String reviewStage) {

		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);
		Long count = baseMapper.selectCount(queryWrapper);

		/**
		 * 初始化，如果已經有分配過審稿委員了，那就別再二次新增
		 */
		if (count > 0) {
			throw new PaperAbstructsException("已存在分配記錄，無法自動分配");
		}

		// 1.獲取全部的稿件
		List<Paper> paperList = paperMapper.selectList(null);

		// 2. 獲取全部評審
		List<PaperReviewer> reviewerList = paperReviewerMapper.selectList(null);

		// 3.如果任一資料為空，就不處理
		if (paperList.isEmpty() || reviewerList.isEmpty()) {
			return; // 無資料不需處理
		}

		// 5. 建立稿件-評審關聯
		List<PaperAndPaperReviewer> relationList = new ArrayList<>();
		for (Paper paper : paperList) {
			String paperAbsType = paper.getAbsType();
			if (paperAbsType == null || paperAbsType.trim().isEmpty()) {
				continue;
			}
			for (PaperReviewer reviewer : reviewerList) {
				String absTypeList = reviewer.getAbsTypeList();
				if (absTypeList == null || absTypeList.trim().isEmpty()) {
					continue;
				}
				// 使用逗號分隔再判斷是否包含 absType
				Set<String> reviewerAbsTypes = Arrays.stream(absTypeList.split(","))
						.map(String::trim)
						.collect(Collectors.toSet());
				if (reviewerAbsTypes.contains(paperAbsType.trim())) {

					PaperAndPaperReviewer relation = new PaperAndPaperReviewer();

					relation.setPaperId(paper.getPaperId());
					relation.setPaperReviewerId(reviewer.getPaperReviewerId());
					relation.setReviewerEmail(reviewer.getEmail());
					relation.setReviewerName(reviewer.getName());

					// 建立關係 第 X 階段審核
					relation.setReviewStage(reviewStage);
					relationList.add(relation);

					// 為評審新增 第X階段 審核者的 分組標籤標籤

					Long currentCount = paperReviewerMapper.selectCount(null);
					int groupSize = GROUP_SIZE;
					int groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

					Tag groupTag = new Tag();
					if (ReviewStageEnum.FIRST_REVIEW.getValue().equals(reviewStage)) {
						groupTag = tagService.getOrCreateFirstReviewerGroupTag(groupIndex);
					} else if (ReviewStageEnum.SECOND_REVIEW.getValue().equals(reviewStage)) {
						groupTag = tagService.getOrCreateSecondReviewerGroupTag(groupIndex);
					} else {
						throw new PaperAbstructsException("沒有對應的階段，無法創建Tag");
					}

					paperReviewerTagService.addPaperReviewerTag(reviewer.getPaperReviewerId(), groupTag.getTagId());

				}
			}
		}

		// 4. 批次插入（批量操作提升效率）
		if (!relationList.isEmpty()) {

			// 方法一，使用mybatis 的 IService方法實現
			this.saveBatch(relationList);

			// 方法二，因為是批量操作，可能有隱式多線程和批次處理，使用Spring 編程式事務 ，mybatis plus 官方推薦
			//			List<BatchResult> execute = transactionTemplate.execute((TransactionCallback<List<BatchResult>>) status -> {
			//				// 使用要批量插入的Mapper , 創建泛型方法
			//				Method<PaperAndPaperReviewer> method = new MybatisBatch.Method<>(PaperAndPaperReviewerMapper.class);
			//				// 返回結果集
			//				return MybatisBatchUtils.execute(sqlSessionFactory, relationList, method.insert());
			//			});

		}

	}

	@Override
	@Transactional
	public void assignPaperReviewerToPaper(String reviewStage, List<Long> targetPaperReviewerIdList, Long paperId) {

		// 1. 查詢當前 paper 在指定審核階段的所有關聯 paperReviewer
		List<PaperAndPaperReviewer> currentPapersAndReviewers = this
				.getPapersAndReviewersByPaperIdAndReviewStage(paperId, reviewStage);

		// 2. 提取當前關聯的 paperReviewerId Set
		//	    這裡只需要獲取與當前 paperId 相關的所有 paperReviewerId，並放入 Set 中以方便比較。
		Set<Long> currentPaperReviewerIdSet = currentPapersAndReviewers.stream()
				.map(PaperAndPaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 3.業務上在 第X階段審核A稿件 審稿委員只會出現一次
		// 第一階段Z委員審核A稿件，這種關係不會出現兩次
		// 為了後續根據 reviewerId 獲取到 PaperAndPaperReviewer關聯 進行精準刪除，
		Map<Long, PaperAndPaperReviewer> currentPaperAndPaperReviewerMapByReviewerId = currentPapersAndReviewers
				.stream()
				.collect(Collectors.toMap(PaperAndPaperReviewer::getPaperReviewerId, Function.identity()));

		// 4.根據type 和 姓名，找到一階段 和 二階段標籤列表，有空再優化
		List<Tag> tagList = tagService.getTagByTypeAndFuzzyName(PAPER_REVIEWER_TYPE, PAPER_REVIEWER_PREFIX);

		// reviewStage 是傳入的 String 參數，首先將其轉換為枚舉
		ReviewStageEnum currentStageEnum = ReviewStageEnum.fromValue(reviewStage);

		// 這邊獲取到 R1 Tag 或 R2 Tag 的標籤Ids
		List<Long> tagIds = tagList.stream()
				.filter(tag -> tag.getName().contains(currentStageEnum.getTagPrefix())) // 直接使用從枚舉獲取的前綴進行判斷
				.map(Tag::getTagId)
				.collect(Collectors.toList());

		// ------------------- 移除操作 ------------------------------

		// 5. 得到該刪除的審稿人ID ,currentPaperReviewerIdSet 中有但 targetPaperReviewerIdSet 中沒有的 ID
		Set<Long> paperReviewerIdsToRemove = new HashSet<>(currentPaperReviewerIdSet); // 建立一個副本，避免修改原始 currentPaperReviewerIdSet
		paperReviewerIdsToRemove.removeAll(new HashSet<>(targetPaperReviewerIdList));

		// 6. 要移除的reviewerIds不為空，執行刪除操作
		if (!paperReviewerIdsToRemove.isEmpty()) {

			// 第一步：獲得審稿關係，並刪除
			this.batchDeletePapersAndReviewers(currentPaperAndPaperReviewerMapByReviewerId, paperReviewerIdsToRemove);

			// 第二步：判斷並刪除 PaperReviewerTag (審稿人資格標籤) 
			// 從這些被移除審稿關係的審稿委員 和 當前審核階段，去查詢是否還有其他 相同階段、不同稿件的情況
			List<Long> reviewerIdsWhoseTagsCanBeRemoved = this
					.getReviewerIdsWhoseTagsCanBeRemoved(paperReviewerIdsToRemove, reviewStage);

			// 當該被移除的審稿委員ID不為空 以及 tags不為空 ，批量刪除這些審稿人的 Tag 關聯
			if (!reviewerIdsWhoseTagsCanBeRemoved.isEmpty() && !tagIds.isEmpty()) {
				this.removeNotUseReviewerTag(reviewerIdsWhoseTagsCanBeRemoved, tagIds);
			}

		}

		// ------------------- 新增操作 ------------------------------

		// 7. 找出需要新增的審稿人ID
		// 這是集合的差集：targetPaperReviewerIdSet - currentPaperReviewerIdSet
		Set<Long> paperReviewerIdsToAdd = new HashSet<>(targetPaperReviewerIdList);
		paperReviewerIdsToAdd.removeAll(currentPaperReviewerIdSet);

		// 7. 如果要新增的審稿人ID不為空，開始進行新增操作
		if (!paperReviewerIdsToAdd.isEmpty()) {

			// 第一步：批量新增 PaperAndPaperReviewer 關係
			this.addReviewerToPaper(paperId, reviewStage, paperReviewerIdsToAdd);

			// 第二步：判斷並批量新增 PaperReviewerTag (審稿人資格標籤)
			// 找出這些需要新增的審稿人中，哪些在當前 reviewStage 下 還沒有 對應的 Tag

			this.addTagToReviewer(reviewStage, paperReviewerIdsToAdd);

		}

	}

	/**
	 * 根據 paperId 和 reviewStage 獲得關聯
	 * 
	 * @param paperId     稿件ID
	 * @param reviewStage 審稿階段
	 * @return
	 */
	private List<PaperAndPaperReviewer> getPapersAndReviewersByPaperIdAndReviewStage(Long paperId, String reviewStage) {
		LambdaQueryWrapper<PaperAndPaperReviewer> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperAndPaperReviewer::getPaperId, paperId);
		currentQueryWrapper.eq(PaperAndPaperReviewer::getReviewStage, reviewStage); // 確保在同一個審核階段
		return baseMapper.selectList(currentQueryWrapper);

	}

	/**
	 * 批量刪除 稿件和審稿委員的關聯
	 * 
	 * @param currentPaperAndPaperReviewerMapByReviewerId 審稿者ID和審稿關係的映射
	 * @param paperReviewerIdsToRemove                    要刪除的審稿者ID列表
	 */
	private void batchDeletePapersAndReviewers(
			Map<Long, PaperAndPaperReviewer> currentPaperAndPaperReviewerMapByReviewerId,
			Collection<Long> paperReviewerIdsToRemove) {
		List<Long> relationsIdsToRemove = paperReviewerIdsToRemove.stream().map(reviewerId -> {
			PaperAndPaperReviewer paperAndPaperReviewer = currentPaperAndPaperReviewerMapByReviewerId.get(reviewerId);
			return paperAndPaperReviewer.getPaperAndPaperReviewerId();
		}).collect(Collectors.toList());

		// 批量刪除
		baseMapper.deleteBatchIds(relationsIdsToRemove);
	}

	/**
	 * 根據paperReviewerIdsToRemove 和 reviewStage , 獲得需要刪除審稿人Tag的ID名單
	 * 
	 * @param paperReviewerIdsToRemove 該刪除的審稿人ID列表
	 * @param reviewStage              審稿階段
	 * @return
	 */
	private List<Long> getReviewerIdsWhoseTagsCanBeRemoved(Collection<Long> paperReviewerIdsToRemove,
			String reviewStage) {
		LambdaQueryWrapper<PaperAndPaperReviewer> otherAssignmentsQuery = new LambdaQueryWrapper<>();
		otherAssignmentsQuery.in(PaperAndPaperReviewer::getPaperReviewerId, paperReviewerIdsToRemove);
		otherAssignmentsQuery.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);
		List<PaperAndPaperReviewer> otherRemainingAssignments = baseMapper.selectList(otherAssignmentsQuery);

		// 提取reviewerId
		Set<Long> reviewerIdsStillHavingOtherAssignmentsInThisStage = otherRemainingAssignments.stream()
				.map(PaperAndPaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 從移除審稿關係的審稿委員ID遍歷
		// 如果 查詢的資料中 沒有包含這個reviewerId就把它放進該被移除Tag的審稿委員ID
		return paperReviewerIdsToRemove.stream()
				.filter(reviewerId -> !reviewerIdsStillHavingOtherAssignmentsInThisStage.contains(reviewerId))
				.collect(Collectors.toList());
	}

	/**
	 * 移除沒使用的ReviewerTag
	 * 
	 * @param reviewerIdsWhoseTagsCanBeRemoved 審稿人ID
	 * @param tagIds                           審稿相關的tagIds
	 */
	private void removeNotUseReviewerTag(Collection<Long> reviewerIdsWhoseTagsCanBeRemoved, Collection<Long> tagIds) {
		// 調用批次刪除 Tag 關聯的方法
		List<PaperReviewerTag> paperReviewerTagByReviewerIdsAndTagIds = paperReviewerTagService
				.getPaperReviewerTagByReviewerIdsAndTagIds(reviewerIdsWhoseTagsCanBeRemoved, tagIds);
		List<Long> paperReviewerTagIds = paperReviewerTagByReviewerIdsAndTagIds.stream()
				.map(PaperReviewerTag::getId)
				.collect(Collectors.toList());
		paperReviewerTagService.removeBatchByIds(paperReviewerTagIds);
	}

	/**
	 * 為 處在X階段的 此篇稿件新增審稿人
	 * 
	 * @param paperId               稿件ID
	 * @param reviewStage           審稿階段
	 * @param paperReviewerIdsToAdd 審稿人IDs
	 */
	private void addReviewerToPaper(Long paperId, String reviewStage, Collection<Long> paperReviewerIdsToAdd) {
		// 找到審稿委員列表,且得到 paperReviewerId為key , 實體類為value的映射對象, 方便輸入數值
		List<PaperReviewer> paperReviewerList = paperReviewerMapper.selectBatchIds(paperReviewerIdsToAdd);
		Map<Long, PaperReviewer> paperReviewerById = paperReviewerList.stream()
				.collect(Collectors.toMap(PaperReviewer::getPaperReviewerId, Function.identity()));

		// 批量新增 PaperAndPaperReviewer 關係
		List<PaperAndPaperReviewer> relationsToAdd = paperReviewerIdsToAdd.stream().map(reviewerId -> {
			PaperAndPaperReviewer newRelation = new PaperAndPaperReviewer();
			newRelation.setPaperId(paperId);
			newRelation.setPaperReviewerId(reviewerId);
			newRelation.setReviewStage(reviewStage);
			newRelation.setReviewerName(paperReviewerById.get(reviewerId).getName());
			newRelation.setReviewerEmail(paperReviewerById.get(reviewerId).getEmail());
			// 其他必要的屬性設置，例如創建時間、狀態等
			return newRelation;
		}).collect(Collectors.toList());

		// 執行批量新增
		// 假設 baseMapper.saveBatch() 支援 List<Entity> 的批量插入
		this.saveBatch(relationsToAdd);
	}

	/**
	 * 為審稿人新增 審稿標籤
	 * 
	 * @param reviewStage           審稿階段
	 * @param paperReviewerIdsToAdd 審稿人IDs
	 */
	private void addTagToReviewer(String reviewStage, Collection<Long> paperReviewerIdsToAdd) {
		// 獲取當前審稿人總數以確定分組
		Long currentCount = paperReviewerMapper.selectCount(null);
		int groupSize = GROUP_SIZE;
		int groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

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
			paperReviewerTagService.addPaperReviewerTagsBatch(paperReviewerIdsToAdd, groupTag.getTagId());
		}
	}

	@Override
	public void submitReviewScore(PutPaperReviewDTO putPaperReviewDTO) {
		PaperAndPaperReviewer putPaperAndPaperReviewer = paperAndPaperReviewerConvert.putDTOToEntity(putPaperReviewDTO);
		baseMapper.updateById(putPaperAndPaperReviewer);

	}

	@Override
	public Boolean isReviewFinished(String reviewStage, Long paperReviewerId) {
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getReviewStage, reviewStage)
				.eq(PaperAndPaperReviewer::getPaperReviewerId, paperReviewerId)
				.isNull(PaperAndPaperReviewer::getScore);

		Long count = baseMapper.selectCount(queryWrapper);

		if (count == 0) {
			return true;
		}

		return false;
	}

}
