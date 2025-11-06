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
import tw.com.topbs.helper.TagAssignmentHelper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.excelPojo.PaperScoreExcel;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.NotificationService;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;

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
	private final TagAssignmentHelper tagAssignmentHelper;

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
	private static final Integer AWARDED = PaperStatusEnum.AWARDED.getValue();
	private static final Integer NOT_AWARDED = PaperStatusEnum.NOT_AWARDED.getValue();

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
			this.entry(UNREVIEWED, ACCEPTED, this::addAcceptedTag),
			this.entry(UNREVIEWED, REJECTED, this::addRejectedTag), this.entry(ACCEPTED, AWARDED, this::addAwardedTag),
			this.entry(ACCEPTED, NOT_AWARDED, this::addNotAwardedTag),

			// 回退路徑 - 移除標籤（使用 Lambda 適配器）
			this.entry(AWARDED, ACCEPTED, (paperId, groupIndex) -> this.removeAwardedTag(paperId)),
			this.entry(NOT_AWARDED, ACCEPTED, (paperId, groupIndex) -> this.removeNotAwardedTag(paperId)),
			this.entry(ACCEPTED, UNREVIEWED, (paperId, groupIndex) -> this.removeAcceptedTag(paperId)),
			this.entry(REJECTED, UNREVIEWED, (paperId, groupIndex) -> this.removeRejectedTag(paperId)));

	/**
	 * 新增稿件(摘要) 入選 Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addAcceptedTag(Long paperId, int groupIndex) {

		// 1.新增 一階段通過 Tag
		tagAssignmentHelper.assignTagWithIndex(paperId, groupIndex, tagService::getOrCreateAcceptedGroupTag,
				paperTagService::addPaperTag);

		// 2.新增 二階段檔案未傳 Tag
		tagAssignmentHelper.assignTagWithIndex(paperId, groupIndex, tagService::getOrCreateNotSubmittedSlideTag,
				paperTagService::addPaperTag);

	}

	/**
	 * 新增稿件(摘要) 未入選 Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addRejectedTag(Long paperId, int groupIndex) {
		tagAssignmentHelper.assignTagWithIndex(paperId, groupIndex, tagService::getOrCreateRejectedGroupTag,
				paperTagService::addPaperTag);
	}

	/**
	 * 新增稿件 獲獎 Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addAwardedTag(Long paperId, int groupIndex) {
		tagAssignmentHelper.assignTagWithIndex(paperId, groupIndex, tagService::getOrCreateAwardedGroupTag,
				paperTagService::addPaperTag);
	}

	/**
	 * 新增稿件 未獲獎 Tag
	 * 
	 * @param paperId
	 * @param groupIndex
	 */
	private void addNotAwardedTag(Long paperId, int groupIndex) {
		tagAssignmentHelper.assignTagWithIndex(paperId, groupIndex, tagService::getOrCreateNotAwardedGroupTag,
				paperTagService::addPaperTag);
	}

	/**
	 * 移除稿件(摘要) 入選 Tag
	 * 
	 * @param paperId
	 */
	private void removeAcceptedTag(Long paperId) {

		// 1.移除 摘要入選 Tag
		tagAssignmentHelper.removeGroupTagsByPattern(paperId, TagTypeEnum.PAPER.getType(),
				PaperTagEnum.ACCEPTED.getTagName(), tagService::getTagIdsByTypeAndNamePattern,
				paperTagService::removeTagsFromPaper);

		// 2.同時移除 二階段檔案未傳 Tag
		tagAssignmentHelper.removeGroupTagsByPattern(paperId, TagTypeEnum.PAPER.getType(),
				PaperTagEnum.NOT_SUBMITTED_SLIDE.getTagName(), tagService::getTagIdsByTypeAndNamePattern,
				paperTagService::removeTagsFromPaper);

	}

	/**
	 * 移除稿件(摘要) 未入選 Tag
	 * 
	 * @param paperId
	 */
	private void removeRejectedTag(Long paperId) {
		tagAssignmentHelper.removeGroupTagsByPattern(paperId, TagTypeEnum.PAPER.getType(),
				PaperTagEnum.REJECTED.getTagName(), tagService::getTagIdsByTypeAndNamePattern,
				paperTagService::removeTagsFromPaper);
	}

	/**
	 * 移除稿件 獲獎 Tag
	 * 
	 * @param paperId
	 */
	private void removeAwardedTag(Long paperId) {
		tagAssignmentHelper.removeGroupTagsByPattern(paperId, TagTypeEnum.PAPER.getType(),
				PaperTagEnum.AWARDED.getTagName(), tagService::getTagIdsByTypeAndNamePattern,
				paperTagService::removeTagsFromPaper);

	}

	/**
	 * 移除稿件 未獲獎 Tag
	 * 
	 * @param paperId
	 */
	private void removeNotAwardedTag(Long paperId) {
		tagAssignmentHelper.removeGroupTagsByPattern(paperId, TagTypeEnum.PAPER.getType(),
				PaperTagEnum.NOT_AWARDED.getTagName(), tagService::getTagIdsByTypeAndNamePattern,
				paperTagService::removeTagsFromPaper);

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

		// 6.獲取當下與會者群體的Index,進行與會者標籤分組
		tagAssignmentHelper.assignTag(paper.getPaperId(), paperService::getPaperGroupIndex,
				tagService::getOrCreateAttendeesGroupTag, paperTagService::addPaperTag);

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
						AWARDED, // ACCEPTED -> AWARDED(入選 -> 獲獎)
						NOT_AWARDED// ACCEPTED -> NOT_AWARDED(入選 -> 未獲獎)
				),
				// REJECTED 一階段審核駁回 轉換路徑
				REJECTED, Set.of(UNREVIEWED // REJECTED -> UNREVIEWED
				),

				// AWARDED 獲獎 轉換路徑(只允許回去ACCEPTED階段,如要改成NOT_AWARDED 則要兩階段)
				AWARDED, Set.of(ACCEPTED // AWARDED -> ACCEPTED
				),
				// NOT_AWARDED 未獲獎 轉換路徑
				NOT_AWARDED, Set.of(ACCEPTED // NOT_AWARDED -> ACCEPTED
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
	 * --------------------------- 投稿者-二階段稿件操作---------------------------------
	 */

	/**
	 * 初次上傳slide，大檔案切割成分片，最後重新組裝
	 * 
	 * @param addSlideUploadDTO 稿件ID和分片資訊
	 * @param memberId          會員ID
	 * @param file
	 * @return
	 */
	public ChunkResponseVO uploadSlideChunk(@Valid AddSlideUploadDTO addSlideUploadDTO, Long memberId,
			MultipartFile file) {
		// 1.透過paperId 和 memberId 找到特定稿件
		Paper paper = paperService.getPaperByOwner(addSlideUploadDTO.getPaperId(), memberId);

		if (paper == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}

		// 2.上傳稿件(分片)，將稿件資訊、分片資訊、分片檔案，交由 稿件檔案服務處理, 會回傳分片上傳狀態，並在最後一個分片上傳完成時進行合併,新增 進資料庫
		ChunkResponseVO chunkResponseVO = paperFileUploadService.uploadSecondStagePaperFileChunk(paper,
				addSlideUploadDTO, file);

		// 3.如果FilePath 不等於 null , 代表檔案已經合併完成,可以「移除」未繳交二階段檔案的tag
		if (chunkResponseVO.getFilePath() != null) {
			// 移除 二階段檔案未傳 Tag  
			tagAssignmentHelper.removeGroupTagsByPattern(paper.getPaperId(), TagTypeEnum.PAPER.getType(),
					PaperTagEnum.NOT_SUBMITTED_SLIDE.getTagName(), tagService::getTagIdsByTypeAndNamePattern,
					paperTagService::removeTagsFromPaper);
		}

		return chunkResponseVO;
	}

	/**
	 * 更新slide，大檔案切割成分片，最後重新組裝
	 * 
	 * @param putSlideUploadDTO 稿件ID、稿件附件ID和分片資訊
	 * @param memberId          會員ID
	 * @param file              檔案分片
	 * @return
	 */
	public ChunkResponseVO updateSlideChunk(@Valid PutSlideUploadDTO putSlideUploadDTO, Long memberId,
			MultipartFile file) {
		// 1.先靠查詢paperId 和 memberId確定這是稿件本人
		Paper paper = paperService.getPaperByOwner(putSlideUploadDTO.getPaperId(), memberId);

		//如果查不到，報錯
		if (paper == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}

		// 2.更新稿件(分片)，將稿件資訊、分片資訊、分片檔案，交由 稿件檔案服務處理, 會回傳分片上傳狀態，並在最後一個分片上傳完成時進行合併, 更新 進資料庫
		ChunkResponseVO chunkResponseVO = paperFileUploadService.updateSecondStagePaperFileChunk(paper,
				putSlideUploadDTO, file);

		return chunkResponseVO;
	}

	/**
	 * 透過 paperId 和 memberId 確認投稿者在操作此稿件
	 * 並透過 paperFileUploadId 刪除 第二階段 的上傳附件
	 * 
	 * @param paperId
	 * @param memberId
	 * @param paperFileUploadId
	 */
	public void removeSecondStagePaperFile(Long paperId, Long memberId, Long paperFileUploadId) {
		// 1.透過 paperId 和 memberId  獲得指定稿件
		Paper paper = paperService.getPaperByOwner(paperId, memberId);

		// 如果查不到，報錯
		if (paper == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}

		// 2.透過paperFileUploadId 刪除第二階段檔案 (DB 和 Minio)
		paperFileUploadService.removeSecondStagePaperFile(paperId, paperFileUploadId);

		// 3. 移除檔案但它仍是Accpted的稿件,所以要拿到它的groupIndex,為了之後分配tag使用
		int groupIndex = paperService.getPaperGroupIndexByStatus(GROUP_SIZE, PaperStatusEnum.ACCEPTED.getValue());

		// 4. 「新增」 未繳交二階段檔案的tag, 因為檔案刪除後就代表他沒教檔案了
		tagAssignmentHelper.assignTagWithIndex(paperId, groupIndex, tagService::getOrCreateNotSubmittedSlideTag,
				paperTagService::addPaperTag);

	}

	/**
	 * -------------------------- 後台下載相關Excel --------------------------------
	 */

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
