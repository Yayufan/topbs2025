package tw.com.topbs.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.exception.PaperAbstructsException;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.mapper.PaperAndPaperReviewerMapper;
import tw.com.topbs.mapper.PaperFileUploadMapper;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.mapper.PaperTagMapper;
import tw.com.topbs.mapper.SettingMapper;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.MinioUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private static final String ABSTRUCTS_PDF = "abstructs_pdf";
	private static final String ABSTRUCTS_DOCX = "abstructs_docx";

	private final PaperConvert paperConvert;
	private final SettingMapper settingMapper;
	private final MinioUtil minioUtil;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperFileUploadMapper paperFileUploadMapper;
	private final PaperReviewerService paperReviewerService;
	private final AsyncService asyncService;
	private final PaperAndPaperReviewerMapper paperAndPaperReviewerMapper;
	private final PaperTagMapper paperTagMapper;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	@Override
	public PaperVO getPaper(Long paperId) {
		Paper paper = baseMapper.selectById(paperId);
		PaperVO vo = paperConvert.entityToVO(paper);

		// 找尋稿件的附件列表
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

		// 將附件列表塞進vo
		vo.setPaperFileUpload(paperFileUploadList);

		// 找尋符合稿件類別的 可選擇評審名單
		List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
				.getPaperReviewerListByAbsType(vo.getAbsType());
		// 將可選擇評審名單塞進vo
		vo.setAvailablePaperReviewers(paperReviewerListByAbsType);

		return vo;
	}

	@Override
	public PaperVO getPaper(Long paperId, Long memberId) {
		// 找到memberId 和 paperId 都符合的唯一數據
		// memberId是避免他去搜尋到別人的數據
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId).eq(Paper::getPaperId, paperId);

		Paper paper = baseMapper.selectOne(paperQueryWrapper);
		PaperVO vo = paperConvert.entityToVO(paper);

		// 找尋稿件的附件列表
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);

		// 將附件列表塞進vo
		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);
		vo.setPaperFileUpload(paperFileUploadList);

		return vo;
	}

	@Override
	public List<PaperVO> getPaperList(Long memberId) {
		// 找到符合memberId 的列表數據
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId);

		List<Paper> paperList = baseMapper.selectList(paperQueryWrapper);

		List<PaperVO> voList = paperList.stream().map(paper -> {

			// 找尋稿件的附件列表
			LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
			paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId());
			List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

			// 將附件列表塞進vo
			PaperVO vo = paperConvert.entityToVO(paper);
			vo.setPaperFileUpload(paperFileUploadList);

			return vo;
		}).collect(Collectors.toList());

		return voList;
	}

	@Override
	public List<Paper> getPaperList() {
		List<Paper> paperList = baseMapper.selectList(null);
		return paperList;
	}

	@Override
	public IPage<PaperVO> getPaperPage(Page<Paper> pageable) {
		// 先透過page分頁拿到對應Paper(稿件)的分頁情況
		Page<Paper> paperPage = baseMapper.selectPage(pageable, null);

		// 取出page對象中的record紀錄
		List<Paper> paperList = paperPage.getRecords();

		// 對paperList做stream流處理
		List<PaperVO> voList = paperList.stream().map(paper -> {

			// 找尋稿件的附件列表
			LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
			paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId());
			List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

			// 將附件列表塞進vo
			PaperVO vo = paperConvert.entityToVO(paper);
			vo.setPaperFileUpload(paperFileUploadList);

			// 找尋符合稿件類別的 可選擇評審名單
			List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
					.getPaperReviewerListByAbsType(vo.getAbsType());

			// 將可選擇評審名單塞進vo
			vo.setAvailablePaperReviewers(paperReviewerListByAbsType);

			return vo;

		}).collect(Collectors.toList());

		// 創建PaperVO 類型的 vo對象
		Page<PaperVO> voPage = new Page<>(paperPage.getCurrent(), paperPage.getSize(), paperPage.getTotal());

		// 將voList設定至records屬性
		voPage.setRecords(voList);

		return voPage;
	}

	@Override
	public IPage<PaperVO> getPaperPage(Page<Paper> pageable, String queryText, Integer status, String absType,
			String absProp) {

		// 多條件篩選的組裝
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(StringUtils.isNotBlank(absType), Paper::getAbsType, absType)
				.eq(StringUtils.isNotBlank(absProp), Paper::getAbsProp, absProp)
				.eq(status != null, Paper::getStatus, status)
				// 當 queryText 不為空字串、空格字串、Null 時才加入篩選條件
				.and(StringUtils.isNotBlank(queryText),
						wrapper -> wrapper.like(Paper::getAllAuthor, queryText)
								.or()
								.like(Paper::getAbsTitle, queryText)
								.or()
								.like(Paper::getPublicationGroup, queryText)
								.or()
								.like(Paper::getPublicationNumber, queryText)
								.or()
								.like(Paper::getCorrespondingAuthorPhone, queryText)
								.or()
								.like(Paper::getCorrespondingAuthorEmail, queryText));

		// 開始去組裝paperVO
		// 先透過page分頁拿到對應Paper(稿件)的分頁情況
		Page<Paper> paperPage = baseMapper.selectPage(pageable, paperQueryWrapper);

		// 取出page對象中的record紀錄
		List<Paper> paperList = paperPage.getRecords();

		// 對paperList做stream流處理
		List<PaperVO> voList = paperList.stream().map(paper -> {

			// 找尋稿件的附件列表
			LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
			paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId());
			List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

			// 將附件列表塞進vo
			PaperVO vo = paperConvert.entityToVO(paper);
			vo.setPaperFileUpload(paperFileUploadList);

			// 找尋符合稿件類別的 可選擇評審名單
			List<PaperReviewer> paperReviewerListByAbsType = paperReviewerService
					.getPaperReviewerListByAbsType(vo.getAbsType());

			// 將可選擇評審名單塞進vo
			vo.setAvailablePaperReviewers(paperReviewerListByAbsType);

			return vo;

		}).collect(Collectors.toList());

		// 創建PaperVO 類型的 vo對象
		Page<PaperVO> voPage = new Page<>(paperPage.getCurrent(), paperPage.getSize(), paperPage.getTotal());

		// 將voList設定至records屬性
		voPage.setRecords(voList);

		return voPage;

	}

	@Override
	@Transactional
	public void addPaper(MultipartFile[] files, AddPaperDTO addPaperDTO) {

		// 判斷是否處於能繳交Paper的時段
		Setting setting = settingMapper.selectById(1L);
		LocalDateTime now = LocalDateTime.now();

		// 不符合時段則直接拋出異常
		if (now.isBefore(setting.getAbstractSubmissionStartTime())
				|| now.isAfter(setting.getAbstractSubmissionEndTime())) {
			throw new PaperClosedException("The current time is not within the submission period");
		}

		// 校驗是否通過Abstructs 檔案規範，如果不合規會直接throw Exception
		this.validateAbstructsFiles(files);

		// 新增投稿本身
		Paper paper = paperConvert.addDTOToEntity(addPaperDTO);
		baseMapper.insert(paper);

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
			String path = "paper/abstructs";

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
				addPaperFileUploadDTO.setType(ABSTRUCTS_PDF);

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
				addPaperFileUploadDTO.setType(ABSTRUCTS_DOCX);
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
								<table>
									<tr>
					       				<td >
					           				<img src="https://topbs.zfcloud.cc/_nuxt/banner.DZ8Efg03.png" alt="Conference Banner"  width="640" style="max-width: 100%%; width: 640px; display: block;" object-fit:cover;">
					       				</td>
					   				</tr>
									<tr>
										<td style="font-size:2rem;">Dear Member,</td>
									</tr>
									<tr>
										<td>Thank you for submitting your abstract. We have successfully received your submission.</td>
									</tr>
									<tr>
										<td>Your abstructs details are as follows:</td>
									</tr>
									<tr>
							            <td><strong>Abstructs Title:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Abstructs Type:</strong> %s</td>
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
								</table>
							</body>
						</html>
				"""
				.formatted(paper.getAbsTitle(), paper.getAbsType(), paper.getFirstAuthor(), paper.getSpeaker(),
						paper.getSpeakerAffiliation(), paper.getCorrespondingAuthor(),
						paper.getCorrespondingAuthorEmail(), paper.getCorrespondingAuthorPhone(), paper.getAllAuthor(),
						paper.getAllAuthorAffiliation());

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
		// 判斷是否處於能繳交Paper的時段
		Setting setting = settingMapper.selectById(1L);
		LocalDateTime now = LocalDateTime.now();

		// 不符合時段則直接拋出異常
		if (now.isBefore(setting.getAbstractSubmissionStartTime())
				|| now.isAfter(setting.getAbstractSubmissionEndTime())) {
			throw new PaperClosedException("The current time is not within the submission period");
		}

		// 校驗是否通過Abstructs 檔案規範，如果不合規會直接throw Exception
		this.validateAbstructsFiles(files);

		// 獲取更新投稿的資訊並修改投稿本身
		Paper currentPaper = paperConvert.putDTOToEntity(putPaperDTO);
		baseMapper.updateById(currentPaper);

		// 接下來找到屬於這篇稿件的，有關ABSTRUCTS_PDF 和 ABSTRUCTS_DOCX的附件，
		// 這邊雖然跟delete function 很像，但是多了一個查詢條件
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, currentPaper.getPaperId())
				.and(wrapper -> wrapper.eq(PaperFileUpload::getType, ABSTRUCTS_PDF)
						.or()
						.eq(PaperFileUpload::getType, ABSTRUCTS_DOCX));

		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 刪除附件檔案的原本資料
			paperFileUploadMapper.deleteById(paperFileUpload.getPaperFileUploadId());

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
			String path = "paper/abstructs";

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
				addPaperFileUploadDTO.setType(ABSTRUCTS_PDF);

			} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
				path += "/docx/";
				addPaperFileUploadDTO.setType(ABSTRUCTS_DOCX);
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

	}

	@Override
	public void updatePaperForAdmin(PutPaperForAdminDTO puPaperForAdminDTO) {
		Paper paper = paperConvert.putForAdminDTOToEntity(puPaperForAdminDTO);
		baseMapper.updateById(paper);

	};

	@Override
	public void deletePaper(Long paperId) {
		// 先刪除稿件的附檔資料 以及 檔案
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 移除paperFileUpload table 中的資料
			paperFileUploadMapper.deleteById(paperFileUpload);
		}

		// 最後才刪除此稿件資料
		baseMapper.deleteById(paperId);

	}

	@Override
	public void deletePaper(Long paperId, Long memberId) {

		// 找到memberId 和 paperId 都符合的唯一數據
		// memberId是避免他去搜尋到別人的數據
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId).eq(Paper::getPaperId, paperId);

		// 這邊有獲取到的Paper才算會員真的有這筆投稿資料
		Paper paper = baseMapper.selectOne(paperQueryWrapper);

		// 先刪除稿件的附檔資料 以及 檔案
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId());
		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 移除paperFileUpload table 中的資料
			paperFileUploadMapper.deleteById(paperFileUpload);
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
	private void validateAbstructsFiles(MultipartFile[] files) {
		// 檔案存在，校驗檔案是否符合規範，單個檔案不超過20MB，
		if (files != null && files.length > 0) {
			// 開始遍歷處理檔案
			for (MultipartFile file : files) {
				// 檢查檔案大小 (20MB = 20 * 1024 * 1024)
				if (file.getSize() > 20 * 1024 * 1024) {
					throw new PaperAbstructsException("A single file exceeds 20MB");
				}

				// 檢查檔案類型
				String contentType = file.getContentType();
				if (!"application/pdf".equals(contentType) && !"application/msword".equals(contentType)
						&& !"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
								.equals(contentType)) {
					throw new PaperAbstructsException("File format only supports PDF and Word files");
				}

			}
		}

	}

	@Transactional
	@Override
	public void assignPaperReviewerToPaper(List<Long> targetPaperReviewerIdList, Long paperId) {

		// 1. 查詢當前 paper 的所有關聯 paperReviewer
		LambdaQueryWrapper<PaperAndPaperReviewer> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperAndPaperReviewer::getPaperId, paperId);
		List<PaperAndPaperReviewer> currentPaperAndPaperReviewerList = paperAndPaperReviewerMapper
				.selectList(currentQueryWrapper);

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
			paperAndPaperReviewerMapper.delete(deletePaperAndPaperReviewerWrapper);
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
				paperAndPaperReviewerMapper.insert(paperAndPaperReviewer);
			}
		}

	}

	@Override
	public void assignTagToPaper(List<Long> targetTagIdList, Long paperId) {
		// 1. 查詢當前 paper 的所有關聯 tag
		LambdaQueryWrapper<PaperTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperTag::getPaperId, paperId);
		List<PaperTag> currentPaperTags = paperTagMapper.selectList(currentQueryWrapper);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentPaperTags.stream().map(PaperTag::getTagId).collect(Collectors.toSet());

		// 3. 對比目標 paperIdList 和當前 paperIdList
		Set<Long> targetTagIdSet = new HashSet<>(targetTagIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> tagsToRemove = new HashSet<>(currentTagIdSet);
		// 差集：當前有但目標沒有
		tagsToRemove.removeAll(targetTagIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> tagsToAdd = new HashSet<>(targetTagIdSet);
		// 差集：目標有但當前沒有
		tagsToAdd.removeAll(currentTagIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!tagsToRemove.isEmpty()) {
			LambdaQueryWrapper<PaperTag> deletePaperTagWrapper = new LambdaQueryWrapper<>();
			deletePaperTagWrapper.eq(PaperTag::getPaperId, paperId).in(PaperTag::getTagId, tagsToRemove);
			paperTagMapper.delete(deletePaperTagWrapper);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			List<PaperTag> newPaperTags = tagsToAdd.stream().map(tagId -> {
				PaperTag paperTag = new PaperTag();
				paperTag.setTagId(tagId);
				paperTag.setPaperId(paperId);
				return paperTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (PaperTag paperTag : newPaperTags) {
				paperTagMapper.insert(paperTag);
			}
		}

	}

}
