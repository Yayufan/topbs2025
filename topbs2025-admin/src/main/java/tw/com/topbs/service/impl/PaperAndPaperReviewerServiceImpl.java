package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;

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

	private final SqlSessionFactory sqlSessionFactory;
	private final PaperMapper paperMapper;
	private final PaperManager paperManager;
	private final PaperConvert paperConvert;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperReviewerMapper paperReviewerMapper;
	private final PaperAndPaperReviewerConvert paperAndPaperReviewerConvert;
	private final TransactionTemplate transactionTemplate;

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

		// 3. 建立稿件-評審關聯
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
					// 第一次建立關係 也是 第一次審核
					relation.setReviewStage(reviewStage);
					relationList.add(relation);
				}
			}
		}

		// 4. 批次插入（批量操作提升效率）
		if (!relationList.isEmpty()) {

			// 僅五次單條insert , DB中會產生五條insert語句紀錄
			//			for (PaperAndPaperReviewer paperAndPaperReviewer : relationList) {
			//				baseMapper.insert(paperAndPaperReviewer);
			//			}

			/**
			 * 批量操作方法一、方法二
			 * JDBC會顯示, 一次Connection 和 一個Preparing , 之後是批量新增的Parameters(幾個元素有幾個)
			 * 
			 * 在DB的general_log 中會產生
			 * 一個 Prepare 語句 INSERT INTO table ( column01,column02,column03 ) VALUES ( ?, ?,
			 * ?)
			 * 一個 Execute 語句 INSERT INTO table ( column01,column02,column03 ) VALUES ( ?, ?,
			 * ?)
			 * 
			 * 儘管只有一個Exceute 語句,VALUES也不是常見的拼接, 但不代表沒有成功實現批量新增
			 * 
			 */

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

		// 1. 查詢當前 paper 的所有關聯 paperReviewer
		LambdaQueryWrapper<PaperAndPaperReviewer> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperAndPaperReviewer::getPaperId, paperId);
		List<PaperAndPaperReviewer> currentPaperAndPaperReviewerList = baseMapper.selectList(currentQueryWrapper);

		// 2. 提取當前關聯的 paperReviewerId Set
		Set<Long> currentPaperReviewerIdSet = currentPaperAndPaperReviewerList.stream()
				.map(PaperAndPaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 3. 對比目標 paperReviewerIdList 和當前 currentPaperReviewerIdSet
		Set<Long> targetPaperReviewerIdSet = new HashSet<>(targetPaperReviewerIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> paperReviewersToRemove = new HashSet<>(currentPaperReviewerIdSet);

		// 差集：當前有但目標沒有
		paperReviewersToRemove.removeAll(targetPaperReviewerIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> paperReviewersToAdd = new HashSet<>(targetPaperReviewerIdSet);
		// 差集：目標有但當前沒有
		paperReviewersToAdd.removeAll(currentPaperReviewerIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!paperReviewersToRemove.isEmpty()) {
			LambdaQueryWrapper<PaperAndPaperReviewer> deletePaperAndPaperReviewerWrapper = new LambdaQueryWrapper<>();
			deletePaperAndPaperReviewerWrapper.eq(PaperAndPaperReviewer::getPaperId, paperId)
					.in(PaperAndPaperReviewer::getPaperReviewerId, paperReviewersToRemove)
					.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);
			baseMapper.delete(deletePaperAndPaperReviewerWrapper);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!paperReviewersToAdd.isEmpty()) {
			List<PaperAndPaperReviewer> newPaperAndPaperReviewers = paperReviewersToAdd.stream()
					.map(paperReviewerId -> {
						PaperAndPaperReviewer paperAndPaperReviewer = new PaperAndPaperReviewer();
						paperAndPaperReviewer.setPaperReviewerId(paperReviewerId);
						paperAndPaperReviewer.setPaperId(paperId);
						//增加審核階段字段
						paperAndPaperReviewer.setReviewStage(reviewStage);
						return paperAndPaperReviewer;
					})
					.collect(Collectors.toList());

			// 批量插入
			for (PaperAndPaperReviewer paperAndPaperReviewer : newPaperAndPaperReviewers) {
				baseMapper.insert(paperAndPaperReviewer);
			}
		}
	}

	@Override
	public void submitReviewScore(PutPaperReviewDTO putPaperReviewDTO) {
		PaperAndPaperReviewer paperAndPaperReviewer = paperAndPaperReviewerConvert.putDTOToEntity(putPaperReviewDTO);
		baseMapper.updateById(paperAndPaperReviewer);
	}

}
