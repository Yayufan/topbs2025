package tw.com.topbs.manager;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperReviewerTagService;
import tw.com.topbs.service.PaperService;

@Component
@RequiredArgsConstructor
public class ReviewerManager {

	private final PaperConvert paperConvert;
	private final PaperService paperService;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperReviewerService paperReviewerService;
	private final PaperReviewerConvert paperReviewerConvert;
	private final PaperReviewerTagService paperReviewerTagService;
	private final PaperReviewerFileService paperReviewerFileService;

	private final PaperAndPaperReviewerService paperAndPaperReviewerService;

	/**
	 * 查詢單一審稿委員
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	public PaperReviewerVO getPaperReviewerVO(Long paperReviewerId) {
		// 1.拿到審稿者資料,轉換成VO對象
		PaperReviewer reviewer = paperReviewerService.getReviewerById(paperReviewerId);
		PaperReviewerVO vo = paperReviewerConvert.entityToVO(reviewer);

		// 2.根據paperReviewerId 獲取Tag, 放入VO
		List<Tag> tagList = paperReviewerTagService.getTagsByPaperReviewerId(paperReviewerId);
		vo.setTagList(tagList);

		// 3.根據paperReviewerId 獲取PaperReviewerFile , 放入VO
		List<PaperReviewerFile> paperReviewerFilesByPaperReviewerId = paperReviewerFileService
				.getReviewerFilesByReviewerId(paperReviewerId);
		vo.setPaperReviewerFileList(paperReviewerFilesByPaperReviewerId);

		return vo;

	}

	/**
	 * 組裝成PaperReviewerVO的私有方法
	 * 
	 * @param reviewerList
	 * @return
	 */
	private List<PaperReviewerVO> buildReviewerVOList(List<PaperReviewer> reviewerList) {

		// 1.獲得審稿委員ID 和 Tag的映射
		Map<Long, List<Tag>> groupTagsByPaperReviewerId = paperReviewerTagService
				.getReviewerTagMapByReviewerId(reviewerList);

		Map<Long, List<PaperReviewerFile>> groupFilesByPaperReviewerId = paperReviewerFileService
				.getReviewerFileMapByReviewerId(reviewerList);

		// 2.遍歷審稿委員名單，轉換成VO
		List<PaperReviewerVO> voList = reviewerList.stream().map(paperReviewer -> {

			PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

			// 根據paperReviewerId 獲取Tag，放入tagList
			vo.setTagList(groupTagsByPaperReviewerId.getOrDefault(paperReviewer.getPaperReviewerId(),
					Collections.emptyList()));

			// 根據paperReviewerId 獲取公文檔案，放入PaperReviewerFileList
			vo.setPaperReviewerFileList(groupFilesByPaperReviewerId.getOrDefault(paperReviewer.getPaperReviewerId(),
					Collections.emptyList()));

			return vo;
		}).collect(Collectors.toList());

		return voList;
	}

	/**
	 * 查詢所有審稿委員
	 * 
	 * @return
	 */
	public List<PaperReviewerVO> getPaperReviewerList() {

		// 1.獲取所有審稿委員
		List<PaperReviewer> reviewers = paperReviewerService.getReviewersEfficiently();

		// 2.組裝成VO
		return this.buildReviewerVOList(reviewers);

	}

	/**
	 * 獲取審稿委員分頁對象
	 * 
	 * @param page
	 * @return
	 */
	public IPage<PaperReviewerVO> getPaperReviewerVOPage(Page<PaperReviewer> page) {
		// 1. 獲取分頁對象
		IPage<PaperReviewer> reviewerPage = paperReviewerService.getReviewerPage(page);

		// 2. 組裝VO
		List<PaperReviewerVO> voList = this.buildReviewerVOList(reviewerPage.getRecords());

		// 3. 將結果塞入page對象
		Page<PaperReviewerVO> voPage = new Page<>(reviewerPage.getCurrent(), reviewerPage.getSize(),
				reviewerPage.getTotal());
		voPage.setRecords(voList);

		return voPage;

	}

	/**
	 * 為 審稿委員 新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param paperReviewerId
	 */
	public void assignTagToReviewer(List<Long> targetTagIdList, Long paperReviewerId) {
		// 1.拿到目標 TagIdSet
		Set<Long> targetTagIdSet = new HashSet<>(targetTagIdList);

		// 2.拿到當前 tagIdSet
		Set<Long> currentTagIdSet = paperReviewerTagService.getTagIdsByReviewerId(paperReviewerId);

		// 3. 找出需要 刪除 / 新增 的關聯關係
		Set<Long> tagsToRemove = Sets.difference(currentTagIdSet, targetTagIdSet);
		Set<Long> tagsToAdd = Sets.difference(targetTagIdSet, currentTagIdSet);

		// 4. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!tagsToRemove.isEmpty()) {
			paperReviewerTagService.removeTagsFromReviewer(paperReviewerId, tagsToRemove);
		}

		// 5. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			paperReviewerTagService.addTagsToReviewer(paperReviewerId, tagsToAdd);
		}

	}

	/**
	 * 根據審稿階段 去查詢 審稿人對應審稿件的評分狀況
	 * 
	 * @param pageable    稿件 和 審稿人的評分關係
	 * @param reviewStage 審稿階段
	 * @return
	 */
	public IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable,
			String reviewStage) {
		return paperAndPaperReviewerService.getReviewerScoreStatsVOPage(pageable, reviewStage);
	}

	/** -------------------------審稿人使用--------------------- */

	/**
	 * 根據審稿委員ID、審核階段，獲得要審稿的稿件對象 (分頁)
	 * 
	 * @param pageable
	 * @param reviewerId
	 * @param reivewStage
	 * @return
	 */
	public IPage<ReviewVO> getReviewVOPageByReviewerIdAndReviewStage(IPage<PaperAndPaperReviewer> pageable,
			Long reviewerId, ReviewStageEnum reviewStageEnum) {

		// 1.查詢符合審稿委員ID、審稿狀態的分頁對象
		IPage<PaperAndPaperReviewer> paperReviewerPage = paperAndPaperReviewerService
				.getPaperReviewersByReviewerIdAndReviewStage(pageable, reviewerId, reviewStageEnum.getValue());

		// 2.如果沒有元素直接返回
		if (paperReviewerPage.getRecords().isEmpty()) {
			return new Page<>(pageable.getCurrent(), pageable.getSize());
		}

		// 3.獲取到稿件IDs, 並以此獲得 稿件的映射對象
		Set<Long> paperIdSet = paperReviewerPage.getRecords()
				.stream()
				.map(PaperAndPaperReviewer::getPaperId)
				.collect(Collectors.toSet());

		Map<Long, Paper> paperMapById = paperService.getPaperMapById(paperIdSet);

		// 4.獲取稿件檔案映射檔案 (透過傳入的參數來決定是第一階段還是第二階段的檔案)
		Map<Long, List<PaperFileUpload>> paperFileMapByPaperIdInReviewStage = paperFileUploadService
				.getPaperFileMapByPaperIdInReviewStage(paperIdSet, reviewStageEnum);

		// 5.遍歷 papersAndReviewersPage 產生 List<ReviewVO> 對象
		List<ReviewVO> reviewVOList = paperReviewerPage.getRecords().stream().map(papersAndReviewers -> {
			ReviewVO reviewVO = paperConvert.entityToReviewVO(paperMapById.get(papersAndReviewers.getPaperId()));
			reviewVO.setFileList(paperFileMapByPaperIdInReviewStage.getOrDefault(papersAndReviewers.getPaperId(),
					Collections.emptyList()));
			reviewVO.setPaperAndPaperReviewerId(papersAndReviewers.getPaperAndPaperReviewerId());
			reviewVO.setScore(papersAndReviewers.getScore());
			return reviewVO;
		}).collect(Collectors.toList());

		// 6.創建一個reviewVOPage 分頁對象回傳
		Page<ReviewVO> reviewVOPage = new Page<>(paperReviewerPage.getCurrent(), paperReviewerPage.getSize(),
				paperReviewerPage.getTotal());
		reviewVOPage.setRecords(reviewVOList);

		return reviewVOPage;

	}

	/**
	 * 審稿委員對稿件進行審核
	 * 
	 * @param putPaperReviewDTO
	 */
	public void submitReviewScore(PutPaperReviewDTO putPaperReviewDTO) {
		paperAndPaperReviewerService.submitReviewScore(putPaperReviewDTO);
	}

}
