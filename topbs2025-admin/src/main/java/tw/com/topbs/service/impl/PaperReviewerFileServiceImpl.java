package tw.com.topbs.service.impl;

import java.util.List;
import java.util.stream.Collectors;

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

	// 附件總大小限制，單位為字節， 20MB，採用10進位制
	private static final long MAX_THREE_FILES_TOTAL_SIZE = 20 * 1000 * 1000;

	// 公文附件基本路徑
	private final String BASE_PATH = "paper-reviewer/offical/";

	// 檔案類型
	private final String OFFICAL_DOCUMENT = "offical-document";

	@Override
	public List<PaperReviewerFile> getPaperReviewerFilesByPaperReviewerId(Long paperReviewerId) {

		LambdaQueryWrapper<PaperReviewerFile> paperReviewerFileWrapper = new LambdaQueryWrapper<>();
		paperReviewerFileWrapper.eq(PaperReviewerFile::getPaperReviewerId, paperReviewerId);

		return baseMapper.selectList(paperReviewerFileWrapper);

	}

	@Override
	public void addPaperReviewerFile(MultipartFile file, Long paperReviewerId) {

		// 1.獲取這個審稿委員的公文附件
		List<PaperReviewerFile> paperReviewerFileList = this.getPaperReviewerFilesByPaperReviewerId(paperReviewerId);

		// 2.判斷是否加入新檔案不超過3個檔案, 且檔案大小不超過20MB
		if (!canAddNewFile(paperReviewerFileList, file)) {
			throw new PaperReviewerFileException("3個檔案超過20MB");
		}

		// 3.開始填充資料
		PaperReviewerFile paperReviewerFile = new PaperReviewerFile();
		paperReviewerFile.setPaperReviewerId(paperReviewerId);
		paperReviewerFile.setFileName(file.getOriginalFilename());
		paperReviewerFile.setType(OFFICAL_DOCUMENT);

		// 4.上傳檔案至Minio,獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
		String uploadUrl = minioUtil.upload(minioBucketName, BASE_PATH, file.getOriginalFilename(), file);
		uploadUrl = "/" + minioBucketName + "/" + uploadUrl;
		paperReviewerFile.setPath(uploadUrl);

		// 5.放入資料庫
		baseMapper.insert(paperReviewerFile);

	}

	/**
	 * 
	 * @param paperReviewerFileList 已有的檔案列表
	 * @param file                  新檔案
	 * @return
	 */
	private Boolean canAddNewFile(List<PaperReviewerFile> paperReviewerFileList, MultipartFile file) {

		// 1.如果當下已經有三個檔案，則直接告訴前端沒有名額了
		if (paperReviewerFileList.size() >= 3) {
			throw new PaperReviewerFileException("超過檔案上限，最多3個檔案");
		}

		// 2.提取已有的檔案path
		List<String> pathList = paperReviewerFileList.stream()
				.map(PaperReviewerFile::getPath)
				.collect(Collectors.toList());

		// 3.判斷當前檔案大小已經多少了
		long calculateTotalSize = minioUtil.calculateTotalSize(pathList);

		// 4.已有檔案 + 新檔案 的大小
		long totalSizeWithNewFile = calculateTotalSize + file.getSize();

		// 5.如果小於20MB返回True , 超過則false
		return totalSizeWithNewFile <= MAX_THREE_FILES_TOTAL_SIZE;
	}

	@Override
	public void updatePaperReviewerFile(MultipartFile file, PutPaperReviewerFileDTO putPaperReviewerFileDTO) {

		// 1.先找到舊的檔案進行刪除
		PaperReviewerFile oldPaperReviewerFile = baseMapper
				.selectById(putPaperReviewerFileDTO.getPaperReviewerFileId());

		// 2.提取舊檔案的minio Path
		String oldFilePath = minioUtil.extractPath(minioBucketName, oldPaperReviewerFile.getPath());

		// 3.獲取這個審稿委員的公文附件
		List<PaperReviewerFile> paperReviewerFileList = this
				.getPaperReviewerFilesByPaperReviewerId(oldPaperReviewerFile.getPaperReviewerId());

		// 4.排除要被更新的檔案，
		List<PaperReviewerFile> remainingFiles = paperReviewerFileList.stream()
				.filter(f -> !f.getPaperReviewerFileId().equals(oldPaperReviewerFile.getPaperReviewerFileId()))
				.collect(Collectors.toList());

		System.out.println("排除要被更新的檔案列表: " + remainingFiles);

		// 5.判斷刪除被替換的檔案 + 新增新檔案，檔案大小是否不超過20MB
		if (!this.canAddNewFile(remainingFiles, file)) {
			throw new PaperReviewerFileException("3個檔案超過20MB");
		}

		// 6.從minio中移除檔案
		minioUtil.removeObject(minioBucketName, oldFilePath);

		// 7.上傳新檔案至Minio,獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
		String uploadUrl = minioUtil.upload(minioBucketName, BASE_PATH, file.getOriginalFilename(), file);
		uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

		// 8.舊紀錄修改檔案資訊 和 檔案路徑
		oldPaperReviewerFile.setFileName(file.getOriginalFilename());
		oldPaperReviewerFile.setPath(uploadUrl);

		// 9.於資料庫中進行修改
		baseMapper.updateById(oldPaperReviewerFile);

	}

	@Override
	public void deletePaperReviewerFile(Long paperFileUploadId) {

		// 1.找到要刪除的審稿委員附件
		PaperReviewerFile paperReviewerFile = baseMapper.selectById(paperFileUploadId);

		// 2.提取路徑
		String filePath = minioUtil.extractPath(minioBucketName, paperReviewerFile.getPath());

		// 3.從minio中移除檔案
		minioUtil.removeObject(minioBucketName, filePath);

		// 4.於資料庫中進行刪除
		baseMapper.deleteById(paperReviewerFile);

	}

}
