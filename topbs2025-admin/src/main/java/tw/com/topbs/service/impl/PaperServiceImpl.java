package tw.com.topbs.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.mapper.SettingMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.MinioUtil;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private final PaperConvert paperConvert;
	private final SettingMapper settingMapper;
	private final MinioUtil minioUtil;
	private final PaperFileUploadService paperFileUploadService;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	@Override
	public Paper getPaper(Long paperId) {
		Paper paper = baseMapper.selectById(paperId);
		return paper;
	}

	@Override
	public List<Paper> getPaperList() {
		List<Paper> paperList = baseMapper.selectList(null);
		return paperList;
	}

	@Override
	public IPage<Paper> getPaperPage(Page<Paper> page) {
		Page<Paper> paperPage = baseMapper.selectPage(page, null);
		return paperPage;
	}

	@Override
	@Transactional
	public void addPaper(MultipartFile[] files, AddPaperDTO addPaperDTO) {
		
		System.out.println("成功校驗");
		
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

				String path = paper.getAbsType() + "/other/";

				// 重新命名檔名
				String fileName = paper.getAbsType() + "_" + paper.getFirstAuthor() + "." + fileExtension;

				// 判斷是PDF檔 還是 DOCX檔 會變更path
				if (fileExtension.equals("pdf")) {
					path = paper.getAbsType() + "/pdf/";
					addPaperFileUploadDTO.setType("pdf");

				} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
					path = paper.getAbsType() + "/docx/";
					addPaperFileUploadDTO.setType("docx");
				}

				// 上傳檔案至Minio,
				// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
				String uploadUrl = minioUtil.upload(minioBucketName, path, fileName, file);
				uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

				// 設定檔案路徑
				addPaperFileUploadDTO.setPath(path);

				// 放入資料庫
				paperFileUploadService.addPaperFileUpload(addPaperFileUploadDTO);

			}

		}
	}

	@Override
	public void updatePaper(PutPaperDTO putPaperDTO) {
		Paper paper = paperConvert.putDTOToEntity(putPaperDTO);
		baseMapper.updateById(paper);

	}

	@Override
	public void deletePaper(Long paperId) {
		baseMapper.deleteById(paperId);

	}

	@Override
	public void deletePaperList(List<Long> paperIds) {
		baseMapper.deleteBatchIds(paperIds);

	}

	// 獲取檔案後綴名的方法
	private String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex != -1) {
			return fileName.substring(dotIndex + 1);
		}
		return "";
	}



}
