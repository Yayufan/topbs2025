package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperAndPaperReviewerConvert;
import tw.com.topbs.mapper.PaperAndPaperReviewerMapper;
import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.service.PaperAndPaperReviewerService;

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

	private final PaperAndPaperReviewerConvert paperAndPaperReviewerConvert;

	@Override
	public long getPaperReviewersByReviewStage(String reviewStage) {
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);
		return baseMapper.selectCount(queryWrapper);
	}

	@Override
	public long getReviewerCountByReviewStage(String reviewStage) {
		// 1.查詢該審核階段所有的關聯
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);
		List<PaperAndPaperReviewer> PapersAndReviewers = baseMapper.selectList(queryWrapper);

		// 2.提取ReviewerId,去重,並計算總人數
		return PapersAndReviewers.stream().map(PaperAndPaperReviewer::getPaperReviewerId).distinct().count();

	}

	@Override
	public boolean isReviewerStillAssignedInStage(String reviewStage, Long reviewerId) {
		// 1.查詢該審核階段 , 此審稿人所有的關聯
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getReviewStage, reviewStage)
				.eq(PaperAndPaperReviewer::getPaperReviewerId, reviewerId);
		Long reviewerRelation = baseMapper.selectCount(queryWrapper);

		if (reviewerRelation > 0) {
			return true;
		}

		return false;
	}

	@Override
	public Set<Long> batchCheckReviewersWithoutAssignment(String reviewStage, Collection<Long> reviewerIds) {
		if (reviewerIds.isEmpty()) {
			return Collections.emptySet();
		}

		// 查詢這些審稿人在該階段是否還有其他任務
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.in(PaperAndPaperReviewer::getPaperReviewerId, reviewerIds)
				.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);

		// 獲取在該階段仍有任務的審稿人ID
		Set<Long> reviewersWithAssignments = baseMapper.selectList(queryWrapper)
				.stream()
				.map(PaperAndPaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 返回沒有任何任務的審稿人ID
		return reviewerIds.stream()
				.filter(reviewerId -> !reviewersWithAssignments.contains(reviewerId))
				.collect(Collectors.toSet());
	}

	@Override
	public IPage<PaperAndPaperReviewer> getPaperReviewersByReviewerIdAndReviewStage(
			IPage<PaperAndPaperReviewer> pageable, Long reviewerId, String reviewStage) {
		// 根據paperReviewerId 和 reviewStage查詢應審核稿件
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getPaperReviewerId, reviewerId)
				.eq(PaperAndPaperReviewer::getReviewStage, reviewStage);
		return baseMapper.selectPage(pageable, queryWrapper);

	}

	@Override
	public List<AssignedReviewersVO> getAssignedReviewersByPaperId(Long paperId) {
		LambdaQueryWrapper<PaperAndPaperReviewer> papersAndReviewerWrapper = new LambdaQueryWrapper<>();
		papersAndReviewerWrapper.eq(PaperAndPaperReviewer::getPaperId, paperId);
		List<PaperAndPaperReviewer> papersAndReviewers = baseMapper.selectList(papersAndReviewerWrapper);
		return papersAndReviewers.stream().map(paperAndPaperReviewerConvert::entityToAssignedReviewersVO).toList();
	}

	@Override
	public Map<Long, List<PaperAndPaperReviewer>> groupPaperReviewersByPaperId(String reviewStage) {
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(StringUtils.isNotBlank(reviewStage), PaperAndPaperReviewer::getReviewStage, reviewStage);
		List<PaperAndPaperReviewer> papersReviewers = baseMapper.selectList(queryWrapper);

		return papersReviewers.stream().collect(Collectors.groupingBy(PaperAndPaperReviewer::getPaperId));

	}

	@Override
	public Map<Long, List<AssignedReviewersVO>> getAssignedReviewersMapByPaperId(Collection<Long> paperIds) {

		// 1.如果paperIds為空，返回空Map
		if (paperIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2.查詢符合的 關聯關係
		LambdaQueryWrapper<PaperAndPaperReviewer> papersAndReviewerWrapper = new LambdaQueryWrapper<>();
		papersAndReviewerWrapper.in(PaperAndPaperReviewer::getPaperId, paperIds);
		List<PaperAndPaperReviewer> papersAndReviewers = baseMapper.selectList(papersAndReviewerWrapper);

		// 3.返回paperId為key, assignedReviewersVO 為值的Map
		return papersAndReviewers.stream()
				.map(paperAndPaperReviewerConvert::entityToAssignedReviewersVO) // 轉換成 VO
				.collect(Collectors.groupingBy(AssignedReviewersVO::getPaperId // 按 paperId 分組
				));

	}

	@Override
	public Map<Long, List<AssignedReviewersVO>> getAssignedReviewersMapByPaperId(List<Paper> paperList) {
		List<Long> paperIds = paperList.stream().map(Paper::getPaperId).toList();
		return this.getAssignedReviewersMapByPaperId(paperIds);
	}

	@Override
	public List<PaperAndPaperReviewer> getPapersAndReviewersByReviewerId(Long paperReviewerId) {
		LambdaQueryWrapper<PaperAndPaperReviewer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperAndPaperReviewer::getPaperReviewerId, paperReviewerId);
		return baseMapper.selectList(queryWrapper);
	}

	@Override
	public IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable,
			String reviewStage) {

		return baseMapper.getReviewerScoreStatsPage(pageable, reviewStage);

	}

	/**
	 * 根據 paperId 和 reviewStage 獲得關聯
	 * 
	 * @param paperId     稿件ID
	 * @param reviewStage 審稿階段
	 * @return
	 */
	public List<PaperAndPaperReviewer> getPapersAndReviewersByPaperIdAndReviewStage(Long paperId, String reviewStage) {
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
	public void batchDeletePapersAndReviewers(
			Map<Long, PaperAndPaperReviewer> currentPaperAndPaperReviewerMapByReviewerId,
			Collection<Long> paperReviewerIdsToRemove) {
		List<Long> relationsIdsToRemove = paperReviewerIdsToRemove.stream().map(reviewerId -> {
			PaperAndPaperReviewer paperAndPaperReviewer = currentPaperAndPaperReviewerMapByReviewerId.get(reviewerId);
			return paperAndPaperReviewer.getPaperAndPaperReviewerId();
		}).collect(Collectors.toList());

		// 批量刪除
		baseMapper.deleteBatchIds(relationsIdsToRemove);
	}

	@Override
	public void addReviewerToPaper(Long paperId, String reviewStage, Map<Long, PaperReviewer> reviewerMapById,
			Collection<Long> paperReviewerIdsToAdd) {
		// 1.批量新增 PaperAndPaperReviewer 關係
		List<PaperAndPaperReviewer> relationsToAdd = paperReviewerIdsToAdd.stream().map(reviewerId -> {
			PaperAndPaperReviewer newRelation = new PaperAndPaperReviewer();
			newRelation.setPaperId(paperId);
			newRelation.setPaperReviewerId(reviewerId);
			newRelation.setReviewStage(reviewStage);
			newRelation.setReviewerName(reviewerMapById.get(reviewerId).getName());
			newRelation.setReviewerEmail(reviewerMapById.get(reviewerId).getEmail());
			// 其他必要的屬性設置，例如創建時間、狀態等
			return newRelation;
		}).collect(Collectors.toList());

		// 2.執行批量新增
		this.saveBatch(relationsToAdd);

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
