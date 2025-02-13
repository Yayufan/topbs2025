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

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.exception.PaperClosedException;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.mapper.SettingMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.MinioUtil;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private final PaperConvert paperConvert;
	private final SettingMapper settingMapper;
	private final MinioUtil minioUtil;

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
		// 判斷是否處於能繳交Paper的時段
		Setting setting = settingMapper.selectById(1L);
		LocalDateTime now = LocalDateTime.now();

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
				String originalFilename = file.getOriginalFilename();
				String fileExtension = this.getFileExtension(originalFilename);
				if (fileExtension.equals("pdf")) {
					System.out.println("這是PDF檔案");
					
				} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
					System.out.println("這是Word檔案");
				}
				
				
			}

			List<String> upload = minioUtil.upload(minioBucketName, paper.getAbsType() + "/", files);
			// 基本上只有有一個檔案跟著formData上傳,所以這邊直接寫死,把唯一的url增添進對象中
			String url = upload.get(0);
			// 將bucketName 組裝進url
			url = "/" + minioBucketName + "/" + url;

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
