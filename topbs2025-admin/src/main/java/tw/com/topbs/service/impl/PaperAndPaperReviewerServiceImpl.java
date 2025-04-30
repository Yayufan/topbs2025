package tw.com.topbs.service.impl;

import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.mapper.PaperAndPaperReviewerMapper;
import tw.com.topbs.service.PaperAndPaperReviewerService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 投稿-審稿委員 關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@Service
public class PaperAndPaperReviewerServiceImpl extends ServiceImpl<PaperAndPaperReviewerMapper, PaperAndPaperReviewer>
		implements PaperAndPaperReviewerService {

	@Override
	@Transactional
	public void assignPaperReviewerToPaper(List<Long> targetPaperReviewerIdList, Long paperId) {

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
					.in(PaperAndPaperReviewer::getPaperReviewerId, paperReviewersToRemove);
			baseMapper.delete(deletePaperAndPaperReviewerWrapper);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!paperReviewersToAdd.isEmpty()) {
			List<PaperAndPaperReviewer> newPaperAndPaperReviewers = paperReviewersToAdd.stream()
					.map(paperReviewerId -> {
						PaperAndPaperReviewer paperAndPaperReviewer = new PaperAndPaperReviewer();
						paperAndPaperReviewer.setPaperReviewerId(paperReviewerId);
						paperAndPaperReviewer.setPaperId(paperId);
						return paperAndPaperReviewer;
					})
					.collect(Collectors.toList());

			// 批量插入
			for (PaperAndPaperReviewer paperAndPaperReviewer : newPaperAndPaperReviewers) {
				baseMapper.insert(paperAndPaperReviewer);
			}
		}
	}

}
