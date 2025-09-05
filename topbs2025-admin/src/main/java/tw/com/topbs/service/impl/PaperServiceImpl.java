package tw.com.topbs.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.PaperFileTypeEnum;
import tw.com.topbs.enums.PaperStatusEnum;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.manager.PaperManager;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.AssignedReviewersVO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.pojo.excelPojo.PaperScoreExcel;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.ScheduleEmailTaskService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.utils.MinioUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private final int GROUP_SIZE = 200;

	private final MinioUtil minioUtil;
	private final PaperConvert paperConvert;
	private final PaperManager paperManager;
	private final SettingService settingService;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperReviewerService paperReviewerService;
	private final TagService tagService;
	private final AsyncService asyncService;
	private final PaperTagService paperTagService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;
	private final ScheduleEmailTaskService scheduleEmailTaskService;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public PaperVO getPaper(Long paperId) {

		// 1.根據paperId 查詢資料,並轉換成VO
		Paper paper = baseMapper.selectById(paperId);
		PaperVO vo = paperConvert.entityToVO(paper);

		// 2.找尋稿件的附件列表
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService.getPaperFileUploadListByPaperId(paperId);

		// 3.將附件列表塞進vo
		vo.setPaperFileUpload(paperFileUploadList);

		// 4.找尋符合稿件類別的 可選擇評審名單
		List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
				.getPaperReviewerListByAbsType(vo.getAbsType());

		// 5.將可選擇評審名單塞進vo
		vo.setAvailablePaperReviewers(paperReviewerListByAbsType);

		// 6.根據paperId找到 tagList，並將其塞進VO
		List<Tag> tagList = paperTagService.getTagByPaperId(paperId);

		vo.setTagList(tagList);

		return vo;
	}

	@Override
	public PaperVO getPaper(Long paperId, Long memberId) {
		// 1.找到memberId 和 paperId 都符合的唯一數據，memberId是避免他去搜尋到別人的數據
		Paper paper = this.getPaperByOwner(paperId, memberId);

		// 如果查無資訊直接報錯
		if (paper == null) {
			throw new PaperAbstractsException("Abstracts is not found");
		}

		// 2.如果有數據,轉換成vo對象
		PaperVO vo = paperConvert.entityToVO(paper);

		// 3.找尋稿件的附件列表
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService.getPaperFileUploadListByPaperId(paperId);

		// 4.將附件列表塞進vo
		vo.setPaperFileUpload(paperFileUploadList);

		// 5.因為是For 前端用戶的API,就不給他審核者名單 和 標籤列表
		return vo;
	}

	/**
	 * 傳入paperId 和 memberId 查找特定 Paper
	 * 
	 * @param paperId
	 * @param memberId
	 * @return
	 */
	private Paper getPaperByOwner(Long paperId, Long memberId) {
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId).eq(Paper::getPaperId, paperId);
		return baseMapper.selectOne(paperQueryWrapper);
	};

	@Override
	public List<PaperVO> getPaperList(Long memberId) {
		// 1.找到符合 memberId 的列表數據
		List<Paper> paperList = paperManager.getPaperListByMemberId(memberId);

		// 如果沒有元素則返回空數組
		if (paperList.isEmpty()) {
			return Collections.emptyList();
		}

		// 2.收集所有paperId以便批量查詢附件
		List<Long> paperIds = paperList.stream().map(Paper::getPaperId).collect(Collectors.toList());

		// 3.獲得paperId為key , 檔案附件列表為value的列表
		Map<Long, List<PaperFileUpload>> fileUploadMap = paperFileUploadService.groupFileUploadsByPaperId(paperIds);

		// 4. 組裝VO對象
		List<PaperVO> voList = paperList.stream().map(paper -> {
			PaperVO vo = paperConvert.entityToVO(paper);
			// 從分組的Map中獲取對應附件，如果沒有則返回空列表
			vo.setPaperFileUpload(fileUploadMap.getOrDefault(paper.getPaperId(), Collections.emptyList()));
			return vo;
		}).collect(Collectors.toList());

		// 5.因為是For 前端用戶的API,就不給他審核者名單 和 標籤列表
		return voList;
	}

	@Override
	public IPage<PaperVO> getPaperPage(Page<Paper> pageable) {

		// 1. 先透過page分頁拿到對應Paper(稿件)的分頁情況，提取成List
		Page<Paper> paperPage = baseMapper.selectPage(pageable, null);

		// 2.調用私有方法完成voPage的組建並返回
		return this.buildVOPage(paperPage);
	}

	@Override
	public IPage<PaperVO> getPaperPage(Page<Paper> pageable, String queryText, Integer status, String absType,
			String absProp) {

		// 1.根據查詢條件，得到符合的Paper(稿件)分頁對象
		IPage<Paper> paperPage = paperManager.getPaperPageByQuery(pageable, queryText, status, absType, absProp);

		// 2.調用私有方法完成voPage的組建並返回
		return this.buildVOPage(paperPage);
	}

	private IPage<PaperVO> buildVOPage(IPage<Paper> paperPage) {

		// 1.取出page對象中的List
		List<Paper> paperList = paperPage.getRecords();

		// 2.收集所有 paperId，主鍵唯一不用去重
		List<Long> paperIds = paperList.stream().map(Paper::getPaperId).collect(Collectors.toList());

		// 批量查詢所有相關數據
		// 3.查詢所有附件 (按 paperId 分組)
		Map<Long, List<PaperFileUpload>> fileUploadsGroupByPaperId = paperFileUploadService
				.groupFileUploadsByPaperId(paperIds);
		// 4.查詢所有標籤 (按 paperId 分組)
		Map<Long, List<Tag>> tagsGroupedByPaperId = paperTagService.groupTagsByPaperId(paperIds);

		// 5.查詢所有已分配的審稿委員 (按 paperId 分組)
		Map<Long, List<AssignedReviewersVO>> groupPaperReviewersByPaperId = paperAndPaperReviewerService
				.groupPaperReviewersByPaperId(paperIds);

		// 5.對paperList做stream流處理
		List<PaperVO> voList = paperList.stream().map(paper -> {

			// 轉換成vo
			PaperVO vo = paperConvert.entityToVO(paper);

			// 透過映射找到對應附件列表,放入VO
			vo.setPaperFileUpload(fileUploadsGroupByPaperId.getOrDefault(paper.getPaperId(), Collections.emptyList()));

			// 映射找到 tagList,放入VO
			vo.setTagList(tagsGroupedByPaperId.getOrDefault(paper.getPaperId(), Collections.emptyList()));

			// 找尋符合稿件類別的 可選擇評審名單
			List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
					.getPaperReviewerListByAbsType(vo.getAbsType());

			// 將可選擇評審名單塞進vo
			vo.setAvailablePaperReviewers(paperReviewerListByAbsType);

			// 將已分配的評審名單塞進vo
			vo.setAssignedPaperReviewers(
					groupPaperReviewersByPaperId.getOrDefault(paper.getPaperId(), Collections.emptyList()));

			return vo;

		}).collect(Collectors.toList());

		// 6.創建voPage對象
		Page<PaperVO> voPage = new Page<>(paperPage.getCurrent(), paperPage.getSize(), paperPage.getTotal());

		// 7.將分頁資訊 和 voList 塞入至records屬性
		voPage.setRecords(voList);

		return voPage;
	}

	@Override
	@Transactional
	public void addPaper(MultipartFile[] files, AddPaperDTO addPaperDTO) {

		// 直接呼叫 SettingService 中的方法來判斷摘要投稿是否開放
		if (!settingService.isAbstractSubmissionOpen()) {
			// 如果 isAbstractSubmissionOpen() 返回 false (表示目前不在投稿時段內)
			// 則拋出自定義異常
			throw new PaperClosedException("The current time is not within the submission period");
		}

		// 校驗是否通過Abstracts 檔案規範，如果不合規會直接throw Exception
		this.validateAbstractsFiles(files);

		// 新增投稿本身
		Paper paper = paperConvert.addDTOToEntity(addPaperDTO);
		baseMapper.insert(paper);

		/** ------------------------------------------------------------------ */

		// 為投稿摘要新增 分組標籤
		Long currentCount = paperManager.getPaperCount();
		int groupSize = GROUP_SIZE;
		int groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

		// 拿到分組 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreatePaperGroupTag(groupIndex);

		// 關聯 Paper 與 Tag
		paperTagService.addPaperTag(paper.getPaperId(), groupTag.getTagId());

		/** ------------------------------------------------------------------ */

		// PDF temp file 用於寄信使用
		List<ByteArrayResource> pdfFileList = new ArrayList<>();

		// 再次遍歷檔案，這次進行真實處理
		for (MultipartFile file : files) {

			// 先定義 PaperFileUpload ,並填入paperId 後續組裝使用
			AddPaperFileUploadDTO addPaperFileUploadDTO = new AddPaperFileUploadDTO();
			addPaperFileUploadDTO.setPaperId(paper.getPaperId());

			// 處理檔名和擴展名
			String originalFilename = file.getOriginalFilename();
			String fileExtension = this.getFileExtension(originalFilename);

			// 投稿摘要基本檔案路徑
			String path = "paper/abstracts";

			// 如果presentationType有值，那麼path在增加一節
			if (StringUtils.isNotBlank(paper.getPresentationType())) {
				path += "/" + paper.getPresentationType();
			}

			// absType為必填，所以path固定加上
			path += "/" + paper.getAbsType();

			// 如果absProp有值，那麼path在增加一節
			if (StringUtils.isNotBlank(paper.getAbsProp())) {
				path += "/" + paper.getAbsProp();
			}

			// 重新命名檔名
			String fileName = paper.getAbsType() + "_" + paper.getFirstAuthor() + "." + fileExtension;

			// 判斷是PDF檔 還是 DOCX檔 會變更path
			if (fileExtension.equals("pdf")) {
				path += "/pdf/";
				addPaperFileUploadDTO.setType(PaperFileTypeEnum.ABSTRACTS_PDF.getValue());

				// 使用 ByteArrayResource 轉成 InputStreamSource
				try {
					ByteArrayResource pdfResource = new ByteArrayResource(file.getBytes()) {
						@Override
						public String getFilename() {
							return file.getOriginalFilename(); // 保持檔名正確
						}
					};
					pdfFileList.add(pdfResource); // 儲存到 pdfFileList，供寄信使用

				} catch (Exception e) {
					e.printStackTrace();
					log.error(e.toString());
				}

			} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
				path += "/docx/";
				addPaperFileUploadDTO.setType(PaperFileTypeEnum.ABSTRACTS_DOCX.getValue());
			}

			// 上傳檔案至Minio,
			// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
			String uploadUrl = minioUtil.upload(minioBucketName, path, fileName, file);
			uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

			// 設定檔案路徑
			addPaperFileUploadDTO.setPath(uploadUrl);

			// 放入資料庫
			paperFileUploadService.addPaperFileUpload(addPaperFileUploadDTO);

		}

		// 製作HTML信件
		String htmlContent = """
				<!DOCTYPE html>
						<html >
							<head>
								<meta charset="UTF-8">
								<meta name="viewport" content="width=device-width, initial-scale=1.0">
								<title>Abstract Submission Confirmation</title>
								<style>
								    body { font-size: 1.2rem; line-height: 1.8; }
								    td { padding: 10px 0; }
								</style>
							</head>

							<body >
								<table cellpadding="0" cellspacing="0" >
									<tr>
					       				<td >
					           				<img src="https://iopbs2025.org.tw/_nuxt/banner.CL2lyu9P.png" alt="Conference Banner"  width="640" style="max-width: 100%%; width: 640px; display: block;" object-fit:cover;">
					       				</td>
					   				</tr>
									<tr>
										<td style="font-size:2rem;">Dear %s ,</td>
									</tr>
									<tr>
										<td>Thank you for submitting your abstract. We have successfully received your submission.</td>
									</tr>
									<tr>
										<td>Your abstracts details are as follows:</td>
									</tr>
									<tr>
							            <td><strong>Abstracts Title:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Abstracts Type:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>First Author:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Speaker:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Speaker Affiliation:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Corresponding Author:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Corresponding Author E-Mail:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Corresponding Author Phone:</strong> %s</td>
							        </tr>
							       	<tr>
							            <td><strong>All Author:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>All Author Affiliation:</strong> %s</td>
							        </tr>
									<tr>
										<td>You can still make edits to your submission before the deadline. To make changes</td>
									</tr>
									<tr>
										<td>To avoid confusion, please note that subsequent updates to your submission will not trigger email notifications like this one.</td>
									</tr>
									<tr>
										<td>Your submission will be reviewed, and we will notify you once the results are announced. Please wait for further updates.</td>
									</tr>
									<tr>
										<td><b>This is an automated email. Please do not reply directly to this message.</b></td>
									</tr>
									<tr>
										<td>For any inquiries, please contact iopbs2025@gmail.com</td>
									</tr>
								</table>
							</body>
						</html>
				"""
				.formatted(paper.getCorrespondingAuthor(), paper.getAbsTitle(), paper.getAbsType(),
						paper.getFirstAuthor(), paper.getSpeaker(), paper.getSpeakerAffiliation(),
						paper.getCorrespondingAuthor(), paper.getCorrespondingAuthorEmail(),
						paper.getCorrespondingAuthorPhone(), paper.getAllAuthor(), paper.getAllAuthorAffiliation());

		// 製作純文字信件
		String plainTextContent = """
				Dear Member,

				Thank you for submitting your abstract. We have successfully received your submission.

				Your abstract details are as follows:
				Abstract Title: %s
				Abstract Type: %s
				First Author: %s
				Speaker: %s
				Speaker Affiliation: %s
				Corresponding Author: %s
				Corresponding Author E-Mail: %s
				Corresponding Author Phone: %s
				All Authors: %s
				All Authors Affiliation: %s

				You can still make edits to your submission before the deadline. To make changes, please refer to the provided link.

				To avoid confusion, please note that subsequent updates to your submission will not trigger email notifications like this one.

				Your submission will be reviewed, and we will notify you once the results are announced. Please wait for further updates.

				This is an automated email. Please do not reply directly to this message.

				For any inquiries, please contact iopbs2025@gmail.com.
				"""
				.formatted(paper.getAbsTitle(), paper.getAbsType(), paper.getFirstAuthor(), paper.getSpeaker(),
						paper.getSpeakerAffiliation(), paper.getCorrespondingAuthor(),
						paper.getCorrespondingAuthorEmail(), paper.getCorrespondingAuthorPhone(), paper.getAllAuthor(),
						paper.getAllAuthorAffiliation());

		// 最後去寄一封信給通訊作者(corresponding_author)
		asyncService.sendCommonEmail(paper.getCorrespondingAuthorEmail(),
				"2025 TOPBS & IOPBS Abstract Submission Confirmation", htmlContent, plainTextContent, pdfFileList);

	}

	@Override
	@Transactional
	public void updatePaper(MultipartFile[] files, @Valid PutPaperDTO putPaperDTO) {

		// 直接呼叫 SettingService 中的方法來判斷摘要投稿是否開放
		if (!settingService.isAbstractSubmissionOpen()) {
			// 如果 isAbstractSubmissionOpen() 返回 false (表示目前不在投稿時段內)
			// 則拋出自定義異常
			throw new PaperClosedException("The current time is not within the submission period");
		}

		// 校驗是否通過Abstracts 檔案規範，如果不合規會直接throw Exception
		this.validateAbstractsFiles(files);

		// 獲取更新投稿的資訊並修改投稿本身
		Paper currentPaper = paperConvert.putDTOToEntity(putPaperDTO);
		baseMapper.updateById(currentPaper);

		// 接下來找到屬於這篇稿件的，有關ABSTRACTS_PDF 和 ABSTRACTS_DOCX的附件，
		// 這邊雖然跟delete function 很像，但是多了一個查詢條件
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService
				.getAbstractsByPaperId(currentPaper.getPaperId());

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 刪除附件檔案的原本資料
			paperFileUploadService.deletePaperFileUpload(paperFileUpload.getPaperFileUploadId());

		}

		// 再次遍歷檔案，這次進行真實處理
		for (MultipartFile file : files) {

			// 先定義 PaperFileUpload ,並填入paperId 後續組裝使用
			AddPaperFileUploadDTO addPaperFileUploadDTO = new AddPaperFileUploadDTO();
			addPaperFileUploadDTO.setPaperId(currentPaper.getPaperId());

			// 處理檔名和擴展名
			String originalFilename = file.getOriginalFilename();
			String fileExtension = this.getFileExtension(originalFilename);

			// 投稿摘要基本檔案路徑
			String path = "paper/abstracts";

			// 如果presentationType有值，那麼path在增加一節
			if (StringUtils.isNotBlank(currentPaper.getPresentationType())) {
				path += "/" + currentPaper.getPresentationType();
			}

			// absType為必填，所以path固定加上
			path += "/" + currentPaper.getAbsType();

			// 如果absProp有值，那麼path在增加一節
			if (StringUtils.isNotBlank(currentPaper.getAbsProp())) {
				path += "/" + currentPaper.getAbsProp();
			}

			// 重新命名檔名
			String fileName = currentPaper.getAbsType() + "_" + currentPaper.getFirstAuthor() + "." + fileExtension;

			// 判斷是PDF檔 還是 DOCX檔 會變更path
			if (fileExtension.equals("pdf")) {
				path += "/pdf/";
				addPaperFileUploadDTO.setType(PaperFileTypeEnum.ABSTRACTS_PDF.getValue());

			} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
				path += "/docx/";
				addPaperFileUploadDTO.setType(PaperFileTypeEnum.ABSTRACTS_DOCX.getValue());
			}

			// 上傳檔案至Minio
			// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
			String uploadUrl = minioUtil.upload(minioBucketName, path, fileName, file);
			uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

			// 設定檔案路徑
			addPaperFileUploadDTO.setPath(uploadUrl);

			// 放入資料庫
			paperFileUploadService.addPaperFileUpload(addPaperFileUploadDTO);

		}

	}

	@Override
	public void updatePaperForAdmin(PutPaperForAdminDTO puPaperForAdminDTO) {
		Paper paper = paperConvert.putForAdminDTOToEntity(puPaperForAdminDTO);
		baseMapper.updateById(paper);

		//
		addTagToPaperByStatus(paper.getPaperId(), paper.getStatus());

	};

	private void addTagToPaperByStatus(Long paperId, Integer paperStatus) {
		LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
		paperWrapper.eq(Paper::getStatus, paperStatus);
		Long count = baseMapper.selectCount(paperWrapper);

		// 為投稿摘要新增 分組標籤
		int groupSize = GROUP_SIZE;
		int groupIndex = (int) Math.ceil(count / (double) groupSize);

		Tag groupTag = null;

		// 如果此次變更的稿件狀態，他的值變更為 入選 ，給他新增一個 二階段稿件的Tag
		if (PaperStatusEnum.ACCEPTED.getValue().equals(paperStatus)) {
			groupTag = tagService.getOrCreateSecondPaperGroupTag(groupIndex);
			// 如果此次變更的稿件狀態，他的值變更為 入選(二階段) ，給他新增一個 三階段(最終)稿件的Tag
		} else if (PaperStatusEnum.ACCEPTED_STAGE_2.getValue().equals(paperStatus)) {
			groupTag = tagService.getOrCreateThirdPaperGroupTag(groupIndex);
		}

		if (groupTag != null) {
			paperTagService.addPaperTag(paperId, groupTag.getTagId());
		}

	}

	@Override
	public void deletePaper(Long paperId) {
		// 先刪除稿件的附檔資料 以及 檔案
		// 找尋稿件的附件列表
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService.getPaperFileUploadListByPaperId(paperId);

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 移除paperFileUpload table 中的資料
			paperFileUploadService.deletePaperFileUpload(paperFileUpload.getPaperFileUploadId());

		}

		// 最後才刪除此稿件資料
		baseMapper.deleteById(paperId);

	}

	@Override
	public void deletePaper(Long paperId, Long memberId) {

		// 找到memberId 和 paperId 都符合的唯一數據
		Paper paper = this.getPaperByOwner(paperId, memberId);

		if (paper == null) {
			throw new PaperAbstractsException("Abstrcuts is not found");
		}

		// 先刪除稿件的附檔資料 以及 檔案
		// 找尋稿件的附件列表
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService
				.getPaperFileUploadListByPaperId(paper.getPaperId());

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 移除paperFileUpload table 中的資料
			paperFileUploadService.deletePaperFileUpload(paperFileUpload.getPaperFileUploadId());

		}

		// 最後才刪除此稿件資料
		baseMapper.deleteById(paper.getPaperId());

	}

	@Override
	public void deletePaperList(List<Long> paperIds) {
		// 循環遍歷執行當前Class的deletePaper Function
		for (Long paperId : paperIds) {
			this.deletePaper(paperId);
		}

	}

	/**
	 * 獲取檔案後綴名的方法
	 * 
	 * @param fileName
	 * @return
	 */
	private String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex != -1) {
			return fileName.substring(dotIndex + 1);
		}
		return "";
	}

	/**
	 * 先行校驗單個檔案是否超過20MB，在校驗是否屬於PDF 或者 docx 或者 doc
	 * 
	 * @param files 前端傳來的檔案
	 */
	private void validateAbstractsFiles(MultipartFile[] files) {
		// 檔案存在，校驗檔案是否符合規範，單個檔案不超過20MB，
		if (files != null && files.length > 0) {
			// 開始遍歷處理檔案
			for (MultipartFile file : files) {
				// 檢查檔案大小 (20MB = 20 * 1024 * 1024)
				if (file.getSize() > 20 * 1024 * 1024) {
					throw new PaperAbstractsException("A single file exceeds 20MB");
				}

				// 檢查檔案類型
				String contentType = file.getContentType();
				if (!"application/pdf".equals(contentType) && !"application/msword".equals(contentType)
						&& !"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
								.equals(contentType)) {
					throw new PaperAbstractsException("File format only supports PDF and Word files");
				}

			}
		}

	}

	@Override
	public void assignPaperReviewerToPaper(String reviewStage, List<Long> targetPaperReviewerIdList, Long paperId) {
		paperAndPaperReviewerService.assignPaperReviewerToPaper(reviewStage, targetPaperReviewerIdList, paperId);
	}

	@Override
	public void autoAssignPaperReviewer(String reviewStage) {
		paperAndPaperReviewerService.autoAssignPaperReviewer(reviewStage);
	}

	@Override
	public void assignTagToPaper(List<Long> targetTagIdList, Long paperId) {
		paperTagService.assignTagToPaper(targetTagIdList, paperId);
	}

	@Override
	public void sendEmailToPapers(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);

		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有稿件(通訊作者)
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的稿件(通訊作者)人數
		Long paperCount = 0L;

		//初始化要寄信的稿件(通訊作者)
		List<Paper> paperList = new ArrayList<>();

		//初始化 paperIdSet ，用於去重paperId
		Set<Long> paperIdSet = new HashSet<>();

		if (hasNoTag) {
			paperCount = baseMapper.selectCount(null);
		} else {
			// 透過tag先找到符合的paper關聯
			List<PaperTag> paperTagList = paperTagService.getPaperTagBytagIdList(tagIdList);

			// 從關聯中取出paperId ，使用Set去重複的稿件(通訊作者)，因為稿件(通訊作者)有可能有多個Tag
			paperIdSet = paperTagList.stream().map(paperTag -> paperTag.getPaperId()).collect(Collectors.toSet());

			// 如果paperIdSet 至少有一個，則開始搜尋Member
			if (!paperIdSet.isEmpty()) {
				LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
				paperWrapper.in(Paper::getPaperId, paperIdSet);
				paperCount = baseMapper.selectCount(paperWrapper);
			}

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (currentQuota < paperCount) {
			throw new EmailException("本日寄信額度剩餘: " + currentQuota + "，無法寄送 " + paperCount + " 封信");
		} else if (paperCount <= 0) {
			throw new EmailException("沒有符合資格的稿件(通訊作者)");
		}

		// 前面都已經透過總數先排除了 額度不足、沒有符合資格稿件(通訊作者)的狀況，現在實際來獲取收信者名單
		// 沒有篩選任何Tag的，則給他所有Member名單
		if (hasNoTag) {
			paperList = baseMapper.selectList(null);
		} else {

			// 如果paperIdSet 至少有一個，則開始搜尋Member
			if (!paperIdSet.isEmpty()) {
				LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
				paperWrapper.in(Paper::getPaperId, paperIdSet);
				paperList = baseMapper.selectList(paperWrapper);
			}

		}

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信，這邊是寄給
		asyncService.batchSendEmail(paperList, sendEmailDTO, Paper::getCorrespondingAuthorEmail,
				this::replacePaperMergeTag);

		// 額度直接扣除 查詢到的稿件(通訊作者)數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-paperCount);
	}

	@Override
	public void scheduleEmailToPapers(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {
		// 1.獲取投稿者列表
		List<Paper> paperList = this.getPaperListByTagIds(tagIdList);

		// 2.放入排程任務
		scheduleEmailTaskService.processScheduleEmailTask(sendEmailDTO, paperList, "paper",
				Paper::getCorrespondingAuthorEmail, this::replacePaperMergeTag);

	}

	private List<Paper> getPaperListByTagIds(Collection<Long> tagIdList) {
		// 1.先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有投稿者(通訊作者)
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		// 2.初始化要寄信的投稿者(通訊作者)
		List<Paper> paperList = new ArrayList<>();

		// 3.初始化 paperIdSet ，用於去重paperId
		Set<Long> paperIdSet = new HashSet<>();

		// 4.如果沒給tag代表要寄給全部人，如果有則透過tag找尋要寄送的名單
		if (hasNoTag) {
			paperList = baseMapper.selectList(null);
		} else {

			// 透過tag先找到符合的member關聯

			List<PaperTag> paperTagList = paperTagService.getPaperTagBytagIdList(tagIdList);

			// 從關聯中取出memberId ，使用Set去重複的會員，因為會員有可能有多個Tag
			paperIdSet = paperTagList.stream().map(paperTag -> paperTag.getPaperId()).collect(Collectors.toSet());

			// 如果memberIdSet 至少有一個，則開始搜尋Member
			if (!paperIdSet.isEmpty()) {
				LambdaQueryWrapper<Paper> memberWrapper = new LambdaQueryWrapper<>();
				memberWrapper.in(Paper::getPaperId, paperIdSet);
				paperList = baseMapper.selectList(memberWrapper);
			}

		}

		return paperList;

	}

	@Override
	public String replacePaperMergeTag(String content, Paper paper) {
		String newContent;

		newContent = content.replace("{{absType}}", paper.getAbsType())
				.replace("{{absProp}}", paper.getAbsProp())
				.replace("{{absTitle}}", paper.getAbsTitle())
				.replace("{{firstAuthor}}", paper.getFirstAuthor())
				.replace("{{speaker}}", paper.getSpeaker())
				.replace("{{speakerAffiliation}}", paper.getSpeakerAffiliation())
				.replace("{{correspondingAuthor}}", paper.getCorrespondingAuthor())
				.replace("{{correspondingAuthorEmail}}", paper.getCorrespondingAuthorEmail());

		return newContent;
	}

	/** 以下為入選後，第二階段，查看/上傳/更新 slide、poster、video */

	@Override
	public List<PaperFileUpload> getSecondStagePaperFile(Long paperId, Long memberId) {
		// 1.找到memberId 和 paperId 都符合的唯一數據，memberId是避免他去搜尋到別人的數據
		Paper paper = this.getPaperByOwner(paperId, memberId);

		// 如果查無資訊直接報錯
		if (paper == null) {
			throw new PaperAbstractsException("Abstracts is not found");
		}

		// 2.查找此稿件 第二階段 的附件檔案
		return paperFileUploadService.getSecondStagePaperFilesByPaperId(paperId);

	}

	@Override
	public ChunkResponseVO uploadSlideChunk(AddSlideUploadDTO addSlideUploadDTO, Long memberId, MultipartFile file) {

		// 1.透過paperId 和 memberId 找到特定稿件
		Paper paper = this.getPaperByOwner(addSlideUploadDTO.getPaperId(), memberId);

		if (paper == null) {
			throw new PaperAbstractsException("No matching submissions");
		}

		// 2.上傳稿件(分片)，將稿件資訊、分片資訊、分片檔案，交由 稿件檔案服務處理, 會回傳分片上傳狀態，並在最後一個分片上傳完成時進行合併,新增 進資料庫
		ChunkResponseVO chunkResponseVO = paperFileUploadService.uploadSecondStagePaperFileChunk(paper,
				addSlideUploadDTO, file);

		return chunkResponseVO;
	}

	@Override
	public ChunkResponseVO updateSlideChunk(PutSlideUploadDTO putSlideUploadDTO, Long memberId, MultipartFile file) {
		// 1.先靠查詢paperId 和 memberId確定這是稿件本人
		Paper paper = this.getPaperByOwner(putSlideUploadDTO.getPaperId(), memberId);

		//如果查不到，報錯
		if (paper == null) {
			throw new PaperAbstractsException("No matching submissions");
		}

		// 2.更新稿件(分片)，將稿件資訊、分片資訊、分片檔案，交由 稿件檔案服務處理, 會回傳分片上傳狀態，並在最後一個分片上傳完成時進行合併, 更新 進資料庫
		ChunkResponseVO chunkResponseVO = paperFileUploadService.updateSecondStagePaperFileChunk(paper,
				putSlideUploadDTO, file);

		return chunkResponseVO;
	}

	@Override
	public void removeSecondStagePaperFile(Long paperId, Long memberId, Long paperFileUploadId) {

		// 1.透過 paperId 和 memberId  獲得指定稿件
		Paper paper = this.getPaperByOwner(paperId, memberId);

		// 如果查不到，報錯
		if (paper == null) {
			throw new PaperAbstractsException("No matching submissions");
		}

		// 2.透過paperFileUploadId 刪除第二階段檔案 (DB 和 Minio)
		paperFileUploadService.removeSecondStagePaperFile(paperId, paperFileUploadId);

	}

	/** --------下載稿件評分的Excel---------- */

	@Override
	public void downloadScoreExcel(HttpServletResponse response, String reviewStage) throws IOException {

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");

		// 1.獲得當下階段審核檔名
		String label = ReviewStageEnum.fromValue(reviewStage).getLabel();

		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode(label + "稿件分數", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 2.先查詢所有稿件
		List<Paper> paperList = baseMapper.selectList(null);

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

	}

}
