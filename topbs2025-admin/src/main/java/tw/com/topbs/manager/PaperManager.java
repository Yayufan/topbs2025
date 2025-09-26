package tw.com.topbs.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.PaperStatusEnum;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.NotificationService;
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

	private final int GROUP_SIZE = 200;

	private final PaperService paperService;
	private final PaperConvert paperConvert;
	private final PaperTagService paperTagService;
	private final TagService tagService;
	private final PaperFileUploadService paperFileUploadService;
	private final SettingService settingService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

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

		// 1.直接呼叫 SettingService 中的方法來判斷摘要投稿是否開放
		if (!settingService.isAbstractSubmissionOpen()) {
			// 如果 isAbstractSubmissionOpen() 返回 false (表示目前不在投稿時段內)，則拋出自定義異常
			throw new PaperClosedException("The current time is not within the submission period");
		}

		// 2.校驗是否通過Abstracts 檔案規範，如果不合規會直接throw Exception
		paperService.validateAbstractsFiles(files);

		// 3.新增稿件
		Paper paper = paperService.addPaper(addPaperDTO);

		// 4.新增稿件附件，拿到要放進信件中的PDF檔案
		List<ByteArrayResource> paperPDFFiles = paperFileUploadService.addPaperFileUpload(paper, files);

		// 5.為投稿摘要新增 分組標籤
		int paperGroupIndex = paperService.getPaperGroupIndex(GROUP_SIZE);
		// 拿到分組 Tag（不存在則新增Tag），關聯 Paper 與 Tag
		Tag groupTag = tagService.getOrCreatePaperGroupTag(paperGroupIndex);
		paperTagService.addPaperTag(paper.getPaperId(), groupTag.getTagId());

		// 6.產生通知信件，並寄出給通訊作者
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
			throw new PaperClosedException("The current time is not within the submission period");
		}

		// 2.校驗是否通過Abstracts 檔案規範，如果不合規會直接throw Exception
		paperService.validateAbstractsFiles(files);

		// 3.修改稿件
		Paper paper = paperService.updatePaper(putPaperDTO);

		// 4.修改稿件的附件
		paperFileUploadService.updatePaperFile(paper, files);

		// 只是修改稿件，不用重分組和寄信

	};

	/**
	 * 管理者修改稿件狀態
	 * 
	 * @param putPaperForAdminDTO
	 */
	public void updatePaperForAdmin(PutPaperForAdminDTO putPaperForAdminDTO) {

		// 1.修改稿件
		paperService.updatePaperForAdmin(putPaperForAdminDTO);

		// 2.獲取標籤Index
		int paperGroupIndex = paperService.getPaperGroupIndexByStatus(GROUP_SIZE, putPaperForAdminDTO.getStatus());

		// 3.這邊有問題，不穩定，如果此次變更的稿件狀態，他的值變更為 入選 ，給他新增一個 二階段稿件的Tag
		Tag groupTag = null;
		if (PaperStatusEnum.ACCEPTED.getValue().equals(putPaperForAdminDTO.getStatus())) {
			groupTag = tagService.getOrCreateSecondPaperGroupTag(paperGroupIndex);
			// 如果此次變更的稿件狀態，他的值變更為 入選(二階段) ，給他新增一個 三階段(最終)稿件的Tag
		} else if (PaperStatusEnum.ACCEPTED_STAGE_2.getValue().equals(putPaperForAdminDTO.getStatus())) {
			groupTag = tagService.getOrCreateThirdPaperGroupTag(paperGroupIndex);
		}

		// 4.
		if (groupTag != null) {
			paperTagService.addPaperTag(putPaperForAdminDTO.getPaperId(), groupTag.getTagId());
		}

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

		// 1.校驗是否為稿件的擁有者
		paperService.validateOwner(paperId, memberId);

		// 2.刪除稿件的所有附件
		paperFileUploadService.deletePaperFileByPaperId(paperId);

		// 3.刪除稿件自身
		paperService.deletePaper(paperId);

	}

	public void deletePaperList(List<Long> paperIds) {
		for (Long paperId : paperIds) {
			this.deletePaper(paperId);
		}
	}

}
