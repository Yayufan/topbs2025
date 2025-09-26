package tw.com.topbs.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.excelPojo.PaperScoreExcel;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private final PaperConvert paperConvert;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperTagService paperTagService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public long getPaperCount() {
		return baseMapper.selectCount(null);
	}
	
	@Override
	public long getPaperCountByStatus(Integer status) {
		LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
		paperWrapper.eq(Paper::getStatus, status);
		return baseMapper.selectCount(paperWrapper);
	}

	@Override
	public int getPaperGroupIndex(int groupSize) {
		Long paperCount = this.getPaperCount();
		return (int) Math.ceil(paperCount / (double) groupSize);
	}
	
	@Override
	public int getPaperGroupIndexByStatus(int groupSize, Integer status) {
		long paperCountByStatus = this.getPaperCountByStatus(status);
		return (int) Math.ceil(paperCountByStatus / (double) groupSize);
	}

	@Override
	public Paper getPaper(Long paperId) {
		return baseMapper.selectById(paperId);
	}

	public void validateOwner(Long paperId, Long memberId) {
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId).eq(Paper::getPaperId, paperId);

		// 如果查無資訊直接報錯
		Paper paper = baseMapper.selectOne(paperQueryWrapper);
		if (paper == null) {
			throw new PaperAbstractsException("Abstracts is not found");
		}
	}
	
	@Override
	public Paper getPaper(Long paperId, Long memberId) {
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId).eq(Paper::getPaperId, paperId);

		// 如果查無資訊直接報錯
		Paper paper = baseMapper.selectOne(paperQueryWrapper);
		if (paper == null) {
			throw new PaperAbstractsException("Abstracts is not found");
		}
		return paper;

	}

	@Override
	public List<Paper> getPaperListByIds(Collection<Long> paperIds) {
		if (paperIds.isEmpty()) {
			return Collections.emptyList();
		}
		return baseMapper.selectBatchIds(paperIds);
	}

	@Override
	public List<Paper> getPaperListByMemberId(Long memberId) {
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId);
		return baseMapper.selectList(paperQueryWrapper);
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
	public IPage<Paper> getPaperPageByQuery(Page<Paper> pageable, String queryText, Integer status, String absType,
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

		return baseMapper.selectPage(pageable, paperQueryWrapper);
	}


	@Override
	public Map<Long, Paper> getPaperMapById(List<Long> paperIds) {
		List<Paper> paperList = this.getPaperListByIds(paperIds);
		return paperList.stream().collect(Collectors.toMap(Paper::getPaperId, Function.identity()));
	}

	@Override
	public Paper addPaper(AddPaperDTO addPaperDTO) {
		// 新增投稿本身
		Paper paper = paperConvert.addDTOToEntity(addPaperDTO);
		baseMapper.insert(paper);
		return paper;
	}

	@Override
	public Paper updatePaper(PutPaperDTO putPaperDTO) {
		// 1.校驗是否為用戶
		this.validateOwner(putPaperDTO.getPaperId(),putPaperDTO.getMemberId());
		
		// 2.轉換後並更新
		Paper paper = paperConvert.putDTOToEntity(putPaperDTO);
		baseMapper.updateById(paper);
		return paper;
	}

	@Override
	public void updatePaperForAdmin(PutPaperForAdminDTO puPaperForAdminDTO) {
		Paper paper = paperConvert.putForAdminDTOToEntity(puPaperForAdminDTO);
		baseMapper.updateById(paper);
	};


	@Override
	public void deletePaper(Long paperId) {
		baseMapper.deleteById(paperId);
	}

	@Override
	public void deletePaperList(List<Long> paperIds) {
		// 循環遍歷執行當前Class的deletePaper Function
		for (Long paperId : paperIds) {
			this.deletePaper(paperId);
		}

	}

	/**
	 * 先行校驗單個檔案是否超過20MB，在校驗是否屬於PDF 或者 docx 或者 doc
	 * 
	 * @param files 前端傳來的檔案
	 */
	public void validateAbstractsFiles(MultipartFile[] files) {
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
