package tw.com.topbs.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.exception.PaperReviewerFileException;
import tw.com.topbs.mapper.PaperReviewerFileMapper;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerFileDTO;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.utils.MinioUtil;

/**
 * <p>
 * 給審稿委員的公文檔案和額外]資料 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-06-03
 */
@RequiredArgsConstructor
@Service
public class PaperReviewerFileServiceImpl extends ServiceImpl<PaperReviewerFileMapper, PaperReviewerFile>
		implements PaperReviewerFileService {

	private final MinioUtil minioUtil;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	// 投稿基本路徑
	private final String BASE_PATH = "paper-reviewer/offical/";

	// 檔案類型
	private final String OFFICAL_DOCUMENT = "offical-document";

	@Override
	public void addPaperReviewerFile(MultipartFile file, Long paperReviewerId) {

		// 先判斷有沒有超過三個檔案
		if (this.getCountByPaperReviewerId(paperReviewerId) > 3) {
			throw new PaperReviewerFileException("超過檔案上限，最多3個檔案");
		}

		// 開始填充資料
		PaperReviewerFile paperReviewerFile = new PaperReviewerFile();
		paperReviewerFile.setPaperReviewerId(paperReviewerId);
		paperReviewerFile.setFileName(file.getOriginalFilename());
		paperReviewerFile.setType(OFFICAL_DOCUMENT);

		// 上傳檔案至Minio,
		// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
		String uploadUrl = minioUtil.upload(minioBucketName, BASE_PATH, file.getOriginalFilename(), file);
		uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

		paperReviewerFile.setPath(uploadUrl);

		// 放入資料庫
		baseMapper.insert(paperReviewerFile);

	}

	@Override
	public void updatePaperReviewerFile(MultipartFile file, PutPaperReviewerFileDTO putPaperReviewerFileDTO) {

		// 先找到舊的檔案進行刪除
		PaperReviewerFile oldPaperReviewerFile = baseMapper
				.selectById(putPaperReviewerFileDTO.getPaperReviewerFileId());
		// 提取舊檔案的minio Path
		String oldFilePath = minioUtil.extractPath(minioBucketName, oldPaperReviewerFile.getPath());
		// 從minio中移除檔案
		minioUtil.removeObject(minioBucketName, oldFilePath);

		// 上傳新檔案至Minio,
		// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
		String uploadUrl = minioUtil.upload(minioBucketName, BASE_PATH, file.getOriginalFilename(), file);
		uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

		// 舊紀錄修改檔案資訊 和 檔案路徑
		oldPaperReviewerFile.setFileName(file.getOriginalFilename());
		oldPaperReviewerFile.setPath(uploadUrl);

		// 於資料庫中進行修改
		baseMapper.updateById(oldPaperReviewerFile);

	}

	@Override
	public void deletePaperReviewerFile(Long paperFileUploadId) {

		// 找到要刪除的審稿委員附件
		PaperReviewerFile paperReviewerFile = baseMapper.selectById(paperFileUploadId);

		// 提取路徑
		String filePath = minioUtil.extractPath(minioBucketName, paperReviewerFile.getPath());

		// 從minio中移除檔案
		minioUtil.removeObject(minioBucketName, filePath);

		// 於資料庫中進行刪除
		baseMapper.deleteById(paperReviewerFile);

	}

	/**
	 * 獲取審稿委員 公文附件總數
	 * 
	 * @param paperReviewerId
	 * @return
	 */
	private Long getCountByPaperReviewerId(Long paperReviewerId) {

		LambdaQueryWrapper<PaperReviewerFile> paperReviewerFileWrapper = new LambdaQueryWrapper<>();
		paperReviewerFileWrapper.eq(PaperReviewerFile::getPaperReviewerId, paperReviewerId);

		return baseMapper.selectCount(paperReviewerFileWrapper);

	}

}
