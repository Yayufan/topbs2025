package tw.com.topbs.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.PaperTagVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;

@Component
@RequiredArgsConstructor
public class PaperTagManager {

	private final PaperService paperService;
	private final PaperConvert paperConvert;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperReviewerService paperReviewerService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;
	private final PaperTagService paperTagService;

	/**
	 * 根據paperId獲取PaperTagVO
	 * 
	 * @param paperId
	 * @return
	 */
	public PaperTagVO getPaperTagVO(Long paperId) {

		// 1.先獲取稿件
		Paper paper = paperService.getPaper(paperId);

		// 2.資料轉換
		PaperTagVO paperTagVO = paperConvert.entityToTagVO(paper);

		// 3.找尋稿件的附件列表，將附件列表塞進vo
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService.getPaperFileListByPaperId(paperId);
		paperTagVO.setPaperFileUpload(paperFileUploadList);

		// 4.找尋符合稿件類別的 可選擇評審名單，將可選擇評審名單塞進vo
		List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
				.getReviewerListByAbsType(paperTagVO.getAbsType());
		paperTagVO.setAvailablePaperReviewers(paperReviewerListByAbsType);

		// 5.查詢已分配的審稿委員 (按 paperId 分組)，將已分配評審名單塞進vo
		List<AssignedReviewersVO> assignedReviewers = paperAndPaperReviewerService
				.getAssignedReviewersByPaperId(paperId);
		paperTagVO.setAssignedPaperReviewers(assignedReviewers);

		// 6.根據paperId找到 tagList，並將其塞進VO
		List<Tag> tagList = paperTagService.getTagByPaperId(paperId);
		paperTagVO.setTagList(tagList);

		return paperTagVO;
	}

	/**
	 * 根據條件,獲取PaperTagVO的分頁對象
	 * 
	 * @param pageable  分頁對象
	 * @param queryText 查詢input
	 * @param status    稿件狀態
	 * @param absType   稿件類別
	 * @param absProp   稿件屬性
	 * @return
	 */
	public IPage<PaperTagVO> getPaperTagVOPage(Page<Paper> pageable, String queryText, Integer status, String absType,
			String absProp) {

		// 初始化返回值
		Page<PaperTagVO> voPage = new Page<>(pageable.getCurrent(), pageable.getSize());
		
		// 1.根據條件,獲取paperPage
		IPage<Paper> paperPage = paperService.getPaperPageByQuery(pageable, queryText, status, absType, absProp);

		// 2.如果查無資訊則直接返回
		if(paperPage.getRecords().isEmpty()) {
			return voPage;
		}
		
		// 3.拿到稿件ID 和 稿件 列表的映射對象
		Map<Long, List<PaperFileUpload>> filesMapByPaperId = paperFileUploadService
				.getFilesMapByPaperId(paperPage.getRecords());

		// 4.拿到稿件ID 和 Tag 列表的映射對象
		Map<Long, List<Tag>> tagsMapByPaperId = paperTagService.getTagsMapByPaperId(paperPage.getRecords());

		// 5.拿到稿件ID 和 已分配評審 列表的映射對象
		Map<Long, List<AssignedReviewersVO>> assignedReviewersMapByPaperId = paperAndPaperReviewerService
				.getAssignedReviewersMapByPaperId(paperPage.getRecords());
		
		// 6.對paperPage做stream流處理
		List<PaperTagVO> voList = paperPage.getRecords().stream().map(paper -> {

			// 6-1 轉換成vo
			PaperTagVO vo = paperConvert.entityToTagVO(paper);

			// 6-2 透過映射找到對應附件列表,放入VO
			vo.setPaperFileUpload(filesMapByPaperId.getOrDefault(paper.getPaperId(), Collections.emptyList()));

			// 6-3 映射找到 tagList,放入VO
			vo.setTagList(tagsMapByPaperId.getOrDefault(paper.getPaperId(), Collections.emptyList()));

			// 6-4 暫時不優化,因為AbsType為逗號分隔的字符串，不好查詢，找尋符合稿件類別的 可選擇評審名單
			List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
					.getReviewerListByAbsType(vo.getAbsType());
			// 將可選擇評審名單塞進vo
			vo.setAvailablePaperReviewers(paperReviewerListByAbsType);

			// 6-5 將已分配的評審名單塞進vo
			vo.setAssignedPaperReviewers(
					assignedReviewersMapByPaperId.getOrDefault(paper.getPaperId(), Collections.emptyList()));

			return vo;

		}).collect(Collectors.toList());

		// 7.創建voPage對象
		voPage = new Page<>(paperPage.getCurrent(), paperPage.getSize(), paperPage.getTotal());

		// 8.將分頁資訊 和 voList 塞入至records屬性
		voPage.setRecords(voList);

		return voPage;

	}

}
