package tw.com.topbs.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.mapper.PaperFileUploadMapper;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.mapper.SettingMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.MinioUtil;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private static final String ABSTRUCTS_PDF = "abstructs_pdf";
	private static final String ABSTRUCTS_DOCX = "abstructs_docx";

	private final PaperConvert paperConvert;
	private final SettingMapper settingMapper;
	private final MinioUtil minioUtil;
	private final PaperFileUploadService paperFileUploadService;
	private final PaperFileUploadMapper paperFileUploadMapper;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	@Override
	public PaperVO getPaper(Long paperId) {
		Paper paper = baseMapper.selectById(paperId);
		PaperVO vo = paperConvert.entityToVO(paper);

		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);

		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);
		vo.setPaperFileUpload(paperFileUploadList);

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

		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);

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
			LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
			paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId());

			List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);
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
	public IPage<PaperVO> getPaperPage(Page<Paper> page) {
		// 先透過page分頁拿到對應Paper(稿件)的分頁情況
		Page<Paper> paperPage = baseMapper.selectPage(page, null);

		// 取出page對象中的record紀錄
		List<Paper> paperList = paperPage.getRecords();

		// 對paperList做stream流處理
		List<PaperVO> voList = paperList.stream().map(paper -> {
			LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
			paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId());

			List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);
			PaperVO vo = paperConvert.entityToVO(paper);
			vo.setPaperFileUpload(paperFileUploadList);

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

		// 新增投稿本身
		Paper paper = paperConvert.addDTOToEntity(addPaperDTO);
		baseMapper.insert(paper);

		// 檔案存在，處理檔案
		if (files != null && files.length > 0) {
			// 開始遍歷處理檔案
			for (MultipartFile file : files) {

				// 先定義 PaperFileUpload ,並填入paperId 後續組裝使用
				AddPaperFileUploadDTO addPaperFileUploadDTO = new AddPaperFileUploadDTO();
				addPaperFileUploadDTO.setPaperId(paper.getPaperId());

				// 處理檔名和擴展名
				String originalFilename = file.getOriginalFilename();
				String fileExtension = this.getFileExtension(originalFilename);

				// 基本路徑為:進入投稿/摘要 ，
				String path = "paper/abstructs/";

				// 重新命名檔名
				String fileName = paper.getAbsType() + "_" + paper.getFirstAuthor() + "." + fileExtension;

				// 判斷是PDF檔 還是 DOCX檔 會變更path
				if (fileExtension.equals("pdf")) {
					path += paper.getAbsType() + "/pdf/";
					addPaperFileUploadDTO.setType(ABSTRUCTS_PDF);

				} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
					path += paper.getAbsType() + "/docx/";
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

		// 獲取更新投稿的資訊並修改投稿本身
		Paper currentPaper = paperConvert.putDTOToEntity(putPaperDTO);
		baseMapper.updateById(currentPaper);

		// 接下來找到有關ABSTRUCTS_PDF 和 ABSTRUCTS_DOCX的附件
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, currentPaper.getPaperId()).and(wrapper -> wrapper
				.eq(PaperFileUpload::getType, ABSTRUCTS_PDF).or().eq(PaperFileUpload::getType, ABSTRUCTS_DOCX));

		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

	}

	@Override
	public void deletePaper(Long paperId) {
		// 先刪除稿件的附檔資料 以及 檔案
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		List<PaperFileUpload> paperFileUploadList = paperFileUploadMapper.selectList(paperFileUploadWrapper);

		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = paperFileUpload.getPath().replaceFirst("^/" + minioBucketName + "/", "");

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
			String filePathInMinio = paperFileUpload.getPath().replaceFirst("^/" + minioBucketName + "/", "");

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

}
