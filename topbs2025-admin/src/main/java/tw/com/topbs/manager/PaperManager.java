package tw.com.topbs.manager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.constant.I18nMessageKey;
import tw.com.topbs.context.ProjectModeContext;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.PaperStatusEnum;
import tw.com.topbs.enums.PaperTagEnum;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.enums.TagTypeEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.helper.MessageHelper;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.pojo.excelPojo.PaperScoreExcel;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.NotificationService;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;

/**
 * 處理給投稿者的稿件資訊<br>
 * 僅包含稿件、稿件附件、發表時間、發表地點
 * 
 */
@Component
@RequiredArgsConstructor
@Validated
public class PaperManager {

	@Value("${project.group-size}")
	private int GROUP_SIZE;

	private final ProjectModeContext projectModeContext;
	private final MessageHelper messageHelper;

	private final PaperService paperService;
	private final PaperConvert paperConvert;
	private final PaperTagService paperTagService;
	private final TagService tagService;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;

	private final SettingService settingService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	// 狀態常量，提取是因為太長了,Service不好使用
	private static final Integer UNREVIEWED = PaperStatusEnum.UNREVIEWED.getValue();
	private static final Integer ACCEPTED = PaperStatusEnum.ACCEPTED.getValue();
	private static final Integer REJECTED = PaperStatusEnum.REJECTED.getValue();
	private static final Integer ACCEPTED_STAGE_2 = PaperStatusEnum.ACCEPTED_STAGE_2.getValue();
	private static final Integer REJECTED_STAGE_2 = PaperStatusEnum.REJECTED_STAGE_2.getValue();

	// 群組化標籤的基礎路徑
	private final String GROUP_TAG_BASE_PATH = "-group-";

	/**
	 * 轉換鍵值對
	 */
	private record TransitionKey(Integer fromStatus, Integer toStatus) {
	}

	/**
	 * 輔助方法：創建 Map 條目
	 */
	private Map.Entry<TransitionKey, BiConsumer<Long, Integer>> entry(Integer from, Integer to,
			BiConsumer<Long, Integer> handler) {
		return Map.entry(new TransitionKey(from, to), handler);
	}

	/**
	 * 狀態轉換處理器映射表
	 */
	private final Map<TransitionKey, BiConsumer<Long, Integer>> TRANSITION_HANDLERS = Map.ofEntries(
			// 晉升路徑 - 添加標籤
			this.entry(UNREVIEWED, ACCEPTED, this::addStage1AcceptedTag),
			this.entry(UNREVIEWED, REJECTED, this::addStage1RejectedTag),
			this.entry(ACCEPTED, ACCEPTED_STAGE_2, this::addStage2AcceptedTag),
			this.entry(ACCEPTED, REJECTED_STAGE_2, this::addStage2RejectedTag),

			// 回退路徑 - 移除標籤（使用 Lambda 適配器）
			this.entry(ACCEPTED_STAGE_2, ACCEPTED, (paperId, groupIndex) -> this.removeStage2AcceptedTag(paperId)),
			this.entry(REJECTED_STAGE_2, ACCEPTED, (paperId, groupIndex) -> this.removeStage2RejectedTag(paperId)),
			this.entry(ACCEPTED, UNREVIEWED, (paperId, groupIndex) -> this.removeStage1AcceptedTag(paperId)),
			this.entry(REJECTED, UNREVIEWED, (paperId, groupIndex) -> this.removeStage1RejectedTag(paperId)));

	/**
	 * 新增稿件 一 階段 通過Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addStage1AcceptedTag(Long paperId, int groupIndex) {
		// 1.獲取稿件 一 階段 通過Tag
		Tag groupTag = tagService.getOrCreateAcceptedGroupTag(groupIndex);
		// 2.新增關聯
		paperTagService.addPaperTag(paperId, groupTag.getTagId());
	}

	/**
	 * 新增稿件 二 階段 通過Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addStage2AcceptedTag(Long paperId, int groupIndex) {
		// 1.獲取稿件 二 階段 通過Tag
		Tag groupTag = tagService.getOrCreateAcceptedStage2GroupTag(groupIndex);
		// 2.新增關聯
		paperTagService.addPaperTag(paperId, groupTag.getTagId());
	}

	/**
	 * 新增稿件 一 階段 駁回Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addStage1RejectedTag(Long paperId, int groupIndex) {
		// 1.獲取稿件 一 階段 駁回 Tag
		Tag groupTag = tagService.getOrCreateRejectedGroupTag(groupIndex);
		// 2.新增關聯
		paperTagService.addPaperTag(paperId, groupTag.getTagId());
	}

	/**
	 * 新增稿件 二 階段 駁回Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addStage2RejectedTag(Long paperId, int groupIndex) {
		// 1.獲取稿件二階段 駁回 Tag
		Tag groupTag = tagService.getOrCreateRejectedStage2GroupTag(groupIndex);
		// 2.新增關聯
		paperTagService.addPaperTag(paperId, groupTag.getTagId());
	}

	/**
	 * 通用邏輯，透過tagName pattern 找到符合的tagIds<br>
	 * 並且搭配paperId進行 tag關聯的 刪除
	 * 
	 * @param paperId
	 * @param pattern
	 */
	private void removeTagsByPattern(Long paperId, String pattern) {
		Set<Long> tagIds = tagService.getTagIdsByTypeAndNamePattern(TagTypeEnum.PAPER.getType(),
				pattern + GROUP_TAG_BASE_PATH);
		paperTagService.removeTagsFromPaper(paperId, tagIds);
	}

	/**
	 * 移除稿件 一 階段 通過Tag
	 * 
	 * @param paperId
	 */
	private void removeStage1AcceptedTag(Long paperId) {
		removeTagsByPattern(paperId, PaperTagEnum.ACCEPTED_1.getTagName());
	}

	/**
	 * 移除稿件 二 階段 通過Tag
	 * 
	 * @param paperId
	 */
	private void removeStage2AcceptedTag(Long paperId) {
		removeTagsByPattern(paperId, PaperTagEnum.ACCEPTED_2.getTagName());
	}

	/**
	 * 移除稿件 一 階段 駁回Tag
	 * 
	 * @param paperId
	 */
	private void removeStage1RejectedTag(Long paperId) {
		removeTagsByPattern(paperId, PaperTagEnum.REJECTED_1.getTagName());
	}

	/**
	 * 移除稿件 二 階段 駁回Tag
	 * 
	 * @param paperId
	 */
	private void removeStage2RejectedTag(Long paperId) {
		removeTagsByPattern(paperId, PaperTagEnum.REJECTED_2.getTagName());
	}

	/**
	 * 會員，獲取自身單一稿件
	 * 
	 * @param paperId
	 * @return
	 */
	public PaperVO getPaperVO(Long paperId, Long memberId) {
		// 1.先獲取稿件
		Paper paper = paperService.getPaper(paperId, memberId);
		// 2.資料轉換
		PaperVO paperVO = paperConvert.entityToVO(paper);
		// 3.找尋稿件的附件列表
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService.getPaperFileListByPaperId(paperId);
		// 4.將附件列表塞進vo
		paperVO.setPaperFileUpload(paperFileUploadList);
		return paperVO;
	}

	/**
	 * 會員 獲取自身所有投稿
	 * 
	 * @param memberId
	 * @return
	 */
	public List<PaperVO> getPaperVOList(Long memberId) {
		// 1.先獲取會員的所有 稿件
		List<Paper> paperList = paperService.getPaperListByMemberId(memberId);

		// 2.獲取稿件 與 稿件附件的映射對象
		Map<Long, List<PaperFileUpload>> filesMapByPaperId = paperFileUploadService.getFilesMapByPaperId(paperList);

		// 3.轉換並組裝VO
		List<PaperVO> paperVOList = paperList.stream().map(paper -> {
			// 3-1資料轉換
			PaperVO paperVO = paperConvert.entityToVO(paper);
			// 3-2塞入附件
			paperVO.setPaperFileUpload(filesMapByPaperId.getOrDefault(filesMapByPaperId, Collections.emptyList()));

			return paperVO;
		}).toList();

		return paperVOList;
	};

	/**
	 * 新增稿件
	 * 
	 * @param files
	 * @param addPaperDTO
	 */
	@Transactional
	public void addPaper(MultipartFile[] files, @Valid AddPaperDTO addPaperDTO) {

		// 1.查看當前付款模式,根據策略決定是否阻擋投稿
		projectModeContext.getStrategy().handlePaperSubmission(addPaperDTO.getMemberId());

		// 2.直接呼叫 SettingService 中的方法來判斷摘要投稿是否開放
		if (!settingService.isAbstractSubmissionOpen()) {
			// 如果 isAbstractSubmissionOpen() 返回 false (表示目前不在投稿時段內)，則拋出自定義異常
			throw new PaperClosedException(messageHelper.get(I18nMessageKey.Paper.CLOSED));
		}

		// 3.校驗是否通過Abstracts 檔案規範，如果不合規會直接throw Exception
		paperService.validateAbstractsFiles(files);

		// 4.新增稿件
		Paper paper = paperService.addPaper(addPaperDTO);

		// 5.新增稿件附件，拿到要放進信件中的PDF檔案
		List<ByteArrayResource> paperPDFFiles = paperFileUploadService.addPaperFileUpload(paper, files);

		// 6.為投稿摘要新增 分組標籤
		int paperGroupIndex = paperService.getPaperGroupIndex(GROUP_SIZE);
		// 拿到分組 Tag（不存在則新增Tag），關聯 Paper 與 Tag
		Tag groupTag = tagService.getOrCreatePaperGroupTag(paperGroupIndex);
		paperTagService.addPaperTag(paper.getPaperId(), groupTag.getTagId());

		// 7.產生通知信件，並寄出給通訊作者
		EmailBodyContent abstractSuccessContent = notificationService.generateAbstractSuccessContent(paper);
		asyncService.sendCommonEmail(paper.getCorrespondingAuthorEmail(), "Abstract Submission Confirmation",
				abstractSuccessContent.getHtmlContent(), abstractSuccessContent.getPlainTextContent(), paperPDFFiles);

	}

	/**
	 * 會員修改自身稿件
	 * 
	 * @param files
	 * @param putPaperDTO
	 */
	@Transactional
	public void updatePaper(MultipartFile[] files, @Valid PutPaperDTO putPaperDTO) {
		// 1.直接呼叫 SettingService 中的方法來判斷摘要投稿是否開放
		if (!settingService.isAbstractSubmissionOpen()) {
			// 如果 isAbstractSubmissionOpen() 返回 false (表示目前不在投稿時段內)，則拋出自定義異常
			throw new PaperClosedException(messageHelper.get(I18nMessageKey.Paper.CLOSED));
		}

		// 2.校驗是否通過Abstracts 檔案規範，如果不合規會直接throw Exception
		paperService.validateAbstractsFiles(files);

		// 3.修改稿件
		Paper paper = paperService.updatePaper(putPaperDTO);

		// 4.修改稿件的附件
		paperFileUploadService.updatePaperFile(paper, files);


	};

	/**
	 * 驗證狀態轉換是否合法<br>
	 * 不允許同級互轉，必須回到上一級階段
	 * 
	 */
	private boolean isValidStatusTransition(Integer fromStatus, Integer toStatus) {
		// 定義合法的狀態轉換路徑（嚴格按照您指定的規則）
		Map<Integer, Set<Integer>> allowedTransitions = Map.of(
				// UNREVIEWED 未審核 轉換路徑
				UNREVIEWED, Set.of(ACCEPTED, // UNREVIEWED -> ACCEPTED
						REJECTED // UNREVIEWED -> REJECTED
				),
				// ACCEPTED 一階段審核通過 轉換路徑
				ACCEPTED, Set.of(UNREVIEWED, // ACCEPTED -> UNREVIEWED
						ACCEPTED_STAGE_2, // ACCEPTED -> ACCEPTED_STAGE_2
						REJECTED_STAGE_2// ACCEPTED -> REJECTED_STAGE_2
				),
				// REJECTED 一階段審核駁回 轉換路徑
				REJECTED, Set.of(UNREVIEWED // REJECTED -> UNREVIEWED
				),

				// ACCEPTED_STAGE_2 二階段審核通過 轉換路徑(只允許回去ACCEPTED階段,如要改成REJECTED_STAGE_2 則要兩階段)
				ACCEPTED_STAGE_2, Set.of(ACCEPTED // ACCEPTED_STAGE_2 -> ACCEPTED
				),
				// REJECTED_STAGE_2 二階段審核駁回 轉換路徑
				REJECTED_STAGE_2, Set.of(ACCEPTED // REJECTED_STAGE_2 -> ACCEPTED
				));

		Set<Integer> allowedTargetStates = allowedTransitions.get(fromStatus);
		return allowedTargetStates != null && allowedTargetStates.contains(toStatus);
	}

	/**
	 * 處理狀態轉換時的標籤邏輯
	 */
	private void handleTagTransition(Long paperId, Integer fromStatus, Integer toStatus) {
		int groupIndex = paperService.getPaperGroupIndexByStatus(GROUP_SIZE, toStatus);

		BiConsumer<Long, Integer> handler = TRANSITION_HANDLERS.get(new TransitionKey(fromStatus, toStatus));
		if (handler != null) {
			handler.accept(paperId, groupIndex);
		}
	}

	/**
	 * 管理者修改稿件狀態
	 * 
	 * @param putPaperForAdminDTO
	 */
	public void updatePaperForAdmin(PutPaperForAdminDTO putPaperForAdminDTO) {

		// 1.先獲取當前稿件的狀態和標籤信息
		Paper oldPaper = paperService.getPaper(putPaperForAdminDTO.getPaperId());

		// 2.修改稿件資料
		paperService.updatePaperForAdmin(putPaperForAdminDTO);

		// 3.如果狀態沒有實際變化，直接返回，狀態沒變化代表後面Tag也不需要
		if (oldPaper.getStatus().equals(putPaperForAdminDTO.getStatus())) {
			return;
		}

		// 4.如果稿件狀態轉換不合理則報錯
		if (!this.isValidStatusTransition(oldPaper.getStatus(), putPaperForAdminDTO.getStatus())) {
			throw new PaperAbstractsException(
					"不合規的狀態轉換: " + oldPaper.getStatus() + " -> " + putPaperForAdminDTO.getStatus());
		}

		// 5.如果此次變更的稿件狀態，Tag則根據情況新增或移除
		this.handleTagTransition(putPaperForAdminDTO.getPaperId(), oldPaper.getStatus(),
				putPaperForAdminDTO.getStatus());

	}

	/**
	 * 刪除單一稿件
	 * 
	 * @param paperId
	 * @param memberId
	 */
	public void deletePaper(Long paperId) {

		// 1.刪除稿件的所有附件
		paperFileUploadService.deletePaperFileByPaperId(paperId);

		// 2.刪除稿件自身
		paperService.deletePaper(paperId);

	}

	/**
	 * 會員刪除自身的單一稿件
	 * 
	 * @param paperId
	 * @param memberId
	 */
	public void deletePaper(Long paperId, Long memberId) {

		// 1.直接呼叫 SettingService 中的方法來判斷摘要投稿是否開放
		if (!settingService.isAbstractSubmissionOpen()) {
			// 如果 isAbstractSubmissionOpen() 返回 false (表示目前不在投稿時段內)，則拋出自定義異常
			throw new PaperClosedException(messageHelper.get(I18nMessageKey.Paper.CLOSED));
		}

		// 2.校驗是否為稿件的擁有者
		paperService.validateOwner(paperId, memberId);

		// 3.刪除稿件的所有附件
		paperFileUploadService.deletePaperFileByPaperId(paperId);

		// 4.刪除稿件自身
		paperService.deletePaper(paperId);

	}

	/**
	 * 批量刪除稿件
	 * 
	 * @param paperIds
	 */
	public void deletePaperList(List<Long> paperIds) {
		for (Long paperId : paperIds) {
			this.deletePaper(paperId);
		}
	}

	/**
	 * 下載對應審核階段的稿件評分
	 * 
	 * @param response
	 * @param reviewStage 審核階段
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public void downloadScoreExcel(HttpServletResponse response, String reviewStage)
			throws UnsupportedEncodingException, IOException {

		// 1.初始設定
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		String label = ReviewStageEnum.fromValue(reviewStage).getLabel();
		String fileName = URLEncoder.encode(label + "稿件分數", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 2.查詢所有稿件
		List<Paper> paperList = paperService.getPapersEfficiently();

		// 3.獲得以paperId為key , 關聯紀錄List的映射對象
		Map<Long, List<PaperAndPaperReviewer>> paperReviewersMap = paperAndPaperReviewerService
				.groupPaperReviewersByPaperId(reviewStage);

		// 4.開始遍歷並組裝成Excel對象
		List<PaperScoreExcel> excelData = paperList.stream().map(paper -> {

			PaperScoreExcel paperScoreExcel = paperConvert.entityToExcel(paper);

			// 透過paperId, 獲得他有的所有關聯 (評審 和 分數)
			List<PaperAndPaperReviewer> list = paperReviewersMap.getOrDefault(paper.getPaperId(),
					Collections.emptyList());

			// 新增全部審核人
			String allReviewers = list.stream()
					.map(PaperAndPaperReviewer::getReviewerName)
					.collect(Collectors.joining(","));
			paperScoreExcel.setAllReviewers(allReviewers);

			// 新增有評分的審核人
			String scorers = list.stream()
					.filter(papersReviewers -> papersReviewers.getScore() != null)
					.map(PaperAndPaperReviewer::getReviewerName)
					.collect(Collectors.joining(","));
			paperScoreExcel.setScorers(scorers);

			// 新增所有分數
			String allScores = list.stream()
					.filter(papersReviewers -> papersReviewers.getScore() != null) // 過濾掉 null 的分數
					.map(PaperAndPaperReviewer::getScore) // 取得 Integer 分數
					.map(String::valueOf) // 將 Integer 轉成 String
					.collect(Collectors.joining(",")); // 用逗號連接
			paperScoreExcel.setAllScores(allScores);

			// 新增平均分數
			Double averageScore = list.stream()
					.filter(papersReviewers -> papersReviewers.getScore() != null) // 過濾掉 null 的分數
					.mapToInt(PaperAndPaperReviewer::getScore) // 轉換成 IntStream
					.average() // 計算平均值，回傳 OptionalDouble
					.orElse(0.0); // 如果沒有分數，預設為 0.0
			paperScoreExcel.setAverageScore(averageScore);

			return paperScoreExcel;

		}).collect(Collectors.toList());

		// 5.輸出Excel
		EasyExcel.write(response.getOutputStream(), PaperScoreExcel.class).sheet("稿件分數列表").doWrite(excelData);

	};

}
