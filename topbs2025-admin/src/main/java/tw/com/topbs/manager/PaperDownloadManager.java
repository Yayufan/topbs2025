package tw.com.topbs.manager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.alibaba.excel.EasyExcel;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.constants.PaperFileConstants;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.excelPojo.PaperScoreExcel;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.MinioUtil;

/**
 * 處理稿件相關檔案下載(Excel , 摘要附件 , slide附件)
 */
@Component
@RequiredArgsConstructor
@Validated
public class PaperDownloadManager {

	// 預設存储桶名称
	@Value("${minio.bucketName}")
	private String bucketName;

	private final PaperService paperService;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;
	private final PaperConvert paperConvert;
	private final MinioUtil minioUtil;

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

	/**
	 * 更新稿件流水號，並流式下載所有 摘要 檔案
	 * 
	 * @return
	 */
	public ResponseEntity<StreamingResponseBody> downloadAbstracts() {

		// 1.查詢所有稿件
		List<Paper> paperList = paperService.getPapersEfficiently();

		// 2.命名規則應設
		Map<String, String> renameRules = new HashMap<>();

		// 3.初始化計數器
		AtomicInteger counter = new AtomicInteger(1);

		// 4.拿到稿件ID 和 稿件 列表的映射對象
		Map<Long, List<PaperFileUpload>> filesMapByPaperId = paperFileUploadService.getFilesMapByPaperId(paperList);

		// 5.遍歷稿件,重設流水號
		for (Paper paper : paperList) {
			// 5-1 重設流水號,避免斷號
			paper.setSequenceNo(counter.getAndIncrement());

			// 5-2 根據映射關係,拿到稿件的所有附件
			List<PaperFileUpload> paperFiles = filesMapByPaperId.get(paper.getPaperId());

			// 5-3 遍歷附件,為重命名規則做準備
			for (PaperFileUpload paperFile : paperFiles) {
				// 5-3-1 先拿到檔案原路徑
				String fullPath = paperFile.getPath();
				String minioPath = minioUtil.extractFilePathInMinio(bucketName, fullPath);

				// 5-3-2 獲取副檔名
				String ext = minioUtil.getFileExtension(minioPath);
				// 5-3-3格式化流水號三位數
				String seqStr = String.format("%03d", paper.getSequenceNo());
				// 5-3-4 獲取 類別 與 主講者
				String absType = paper.getAbsType();
				String speaker = paper.getSpeaker();

				// 5-3-5生成新檔名,例:001_oral_林家仰.pdf
				String newFileName = seqStr + "_" + absType + "_" + speaker + ext;

				// 5-3-6 取得原始路徑的資料夾path部分（去掉檔名,包含/線）
				int lastSlash = minioPath.lastIndexOf('/');
				String folderPath = lastSlash != -1 ? minioPath.substring(0, lastSlash + 1) : "";

				// 5-3-7 組成 ZIP 內的完整相對路徑
				String finalRelativePath = folderPath + newFileName;
				// 5-3-8 舊檔案路徑,映射成新檔案路徑 + 名稱
				renameRules.put(minioPath, finalRelativePath);
			}

		}

		// 6.批量更新稿件流水號
		paperService.updateBatchById(paperList);

		// 7.開始生成ZIP檔
		return minioUtil.downloadFolderZipByStream(PaperFileConstants.ABSTRACT_BASE_PATH, renameRules);

	}

	/**
	 * 根據 paper 和附件生成完整 ZIP 檔名，並加入 renameRules
	 */
	private void addRenameRuleForSlide(Paper paper, PaperFileUpload file, Map<String, String> renameRules) {
		String speaker = paper.getSpeaker();
		String fullPath = file.getPath();
		String minioPath = minioUtil.extractFilePathInMinio(bucketName, fullPath);
		String ext = minioUtil.getFileExtension(minioPath);

		String newFileName;
		if (StringUtils.isNotBlank(paper.getPublicationNumber())) {
			newFileName = paper.getPublicationNumber() + "_" + speaker + ext;
		} else {
			String seqStr = String.format("%03d", paper.getSequenceNo());
			newFileName = seqStr + "_" + speaker + ext;
		}

		int lastSlash = minioPath.lastIndexOf('/');
		String folderPath = lastSlash != -1 ? minioPath.substring(0, lastSlash + 1) : "";

		renameRules.put(minioPath, folderPath + newFileName);
	}

	/**
	 * 下載所有 第二階段 上傳的檔案(Slide/Video/Poster)
	 * 
	 * @return
	 */
	public ResponseEntity<StreamingResponseBody> downloadSlides() {

		// 1.查詢所有稿件
		List<Paper> paperList = paperService.getPapersEfficiently();

		// 2.命名規則應設
		Map<String, String> renameRules = new HashMap<>();

		// 3.初始化計數器
		AtomicInteger counter = new AtomicInteger(1);

		// 4.拿到稿件ID 和 稿件 列表的映射對象
		Map<Long, List<PaperFileUpload>> filesMapByPaperId = paperFileUploadService.getFilesMapByPaperId(paperList);

		// 5.遍歷稿件
		for (Paper paper : paperList) {

			// 5-1 根據映射關係,拿到稿件的所有附件
			List<PaperFileUpload> paperFiles = filesMapByPaperId.get(paper.getPaperId());

			// 5-2 重設流水號,避免斷號
			paper.setSequenceNo(counter.getAndIncrement());

			// 5-3遍歷附件,為重命名規則做準備
			for (PaperFileUpload paperFile : paperFiles) {
				// 對附件原檔名進行重命名的映射
				this.addRenameRuleForSlide(paper, paperFile, renameRules);
			}
		}

		// 6.批量更新稿件流水號
		paperService.updateBatchById(paperList);

		// 7.開始生成ZIP檔
		return minioUtil.downloadFolderZipByStream(PaperFileConstants.SLIDE_BASE_PATH, renameRules);

	}

}
