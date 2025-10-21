package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.PaperFileTypeEnum;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.mapper.PaperFileUploadMapper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.service.SysChunkFileService;
import tw.com.topbs.utils.MinioUtil;

@Service
@RequiredArgsConstructor
public class PaperFileUploadServiceImpl extends ServiceImpl<PaperFileUploadMapper, PaperFileUpload>
		implements PaperFileUploadService {

	private final SysChunkFileService sysChunkFileService;

	private final MinioUtil minioUtil;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	private String SECOND_BASE_PATH = "paper/second-stage/";

	@Override
	public PaperFileUpload getPaperFileUpload(Long paperFileUploadId) {
		return baseMapper.selectById(paperFileUploadId);
	}

	@Override
	public List<PaperFileUpload> getPaperFileUploadList() {
		return baseMapper.selectList(null);
	}

	@Override
	public List<PaperFileUpload> getPaperFileListByPaperId(Long paperId) {
		// 找尋稿件的附件列表
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		return baseMapper.selectList(paperFileUploadWrapper);
	}

	@Override
	public List<PaperFileUpload> getPaperFileListByPaperIds(Collection<Long> paperIds) {
		if (paperIds.isEmpty()) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.in(PaperFileUpload::getPaperId, paperIds);
		return baseMapper.selectList(paperFileUploadWrapper);

	}

	@Override
	public List<PaperFileUpload> getPaperFileListByPapers(Collection<Paper> papers) {
		List<Long> paperIds = papers.stream().map(Paper::getPaperId).toList();
		return this.getPaperFileListByPaperIds(paperIds);
	}

	@Override
	public Map<Long, List<PaperFileUpload>> getFilesMapByPaperId(Collection<Paper> papers) {
		List<PaperFileUpload> paperFileList = this.getPaperFileListByPapers(papers);
		return paperFileList.stream().collect(Collectors.groupingBy(PaperFileUpload::getPaperId));
	}

	@Override
	public Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdInReviewStage(Collection<Long> paperIds,
			ReviewStageEnum reviewStageEnum) {
		switch (reviewStageEnum) {
		case FIRST_REVIEW: {

			// 第一階段審稿狀態,返回PDF和Docx檔
			return this.getPaperFileListByPaperIds(paperIds)
					.stream()
					.filter(paperFileUpload -> PaperFileTypeEnum.ABSTRACTS_PDF.getValue()
							.equals(paperFileUpload.getType())
							|| PaperFileTypeEnum.ABSTRACTS_DOCX.getValue().equals(paperFileUpload.getType()))
					.collect(Collectors.groupingBy(PaperFileUpload::getPaperId, // 使用 paperId 作為 key
							Collectors.toList()));
		}
		case SECOND_REVIEW: {
			// 第二階段審稿狀態,返回後續上傳的附件(可能是PDF、PPT、VIDEO)
			return this.getPaperFileListByPaperIds(paperIds)
					.stream()
					.filter(paperFileUpload -> PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue()
							.equals(paperFileUpload.getType()))
					.collect(Collectors.groupingBy(PaperFileUpload::getPaperId, // 使用 paperId 作為 key
							Collectors.toList()));
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + reviewStageEnum);
		}

	}

	@Override
	public Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdAtFirstReviewStage(Collection<Long> paperIds) {
		return this.getPaperFileListByPaperIds(paperIds)
				.stream()
				.filter(paperFileUpload -> PaperFileTypeEnum.ABSTRACTS_PDF.getValue().equals(paperFileUpload.getType())
						|| PaperFileTypeEnum.ABSTRACTS_DOCX.getValue().equals(paperFileUpload.getType()))
				.collect(Collectors.groupingBy(PaperFileUpload::getPaperId, // 使用 paperId 作為 key
						Collectors.toList()));
	}

	@Override
	public Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdAtSecondReviewStage(Collection<Long> paperIds) {
		return this.getPaperFileListByPaperIds(paperIds)
				.stream()
				.filter(paperFileUpload -> PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue()
						.equals(paperFileUpload.getType()))
				.collect(Collectors.groupingBy(PaperFileUpload::getPaperId, // 使用 paperId 作為 key
						Collectors.toList()));
	}

	@Override
	public Map<Long, List<PaperFileUpload>> groupFileUploadsByPaperId(Collection<Long> paperIds) {
		return this.getPaperFileListByPaperIds(paperIds)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(PaperFileUpload::getPaperId));
	}

	@Override
	public List<PaperFileUpload> getAbstractsByPaperId(Long paperId) {
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId)
				.and(wrapper -> wrapper.eq(PaperFileUpload::getType, PaperFileTypeEnum.ABSTRACTS_PDF.getValue())
						.or()
						.eq(PaperFileUpload::getType, PaperFileTypeEnum.ABSTRACTS_DOCX.getValue()));

		return baseMapper.selectList(paperFileUploadWrapper);
	}

	@Override
	public IPage<PaperFileUpload> getPaperFileUploadPage(Page<PaperFileUpload> page) {
		return baseMapper.selectPage(page, null);
	}

	@Override
	public List<ByteArrayResource> addPaperFileUpload(Paper paper, MultipartFile[] files) {
		// PDF temp file 用於寄信使用
		List<ByteArrayResource> pdfFileList = new ArrayList<>();

		// 再次遍歷檔案，這次進行真實處理
		for (MultipartFile file : files) {

			// 先定義 PaperFileUpload ,並填入paperId 後續組裝使用
			PaperFileUpload paperFileUpload = new PaperFileUpload();
			paperFileUpload.setPaperId(paper.getPaperId());

			// 處理檔名和擴展名
			String originalFilename = file.getOriginalFilename();
			String fileExtension = minioUtil.getFileExtension(originalFilename);

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
				paperFileUpload.setType(PaperFileTypeEnum.ABSTRACTS_PDF.getValue());

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
				paperFileUpload.setType(PaperFileTypeEnum.ABSTRACTS_DOCX.getValue());
			}

			// 上傳檔案至Minio,
			// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
			String uploadUrl = minioUtil.upload(minioBucketName, path, fileName, file);
			uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

			// 設定檔案路徑
			paperFileUpload.setPath(uploadUrl);

			// 放入資料庫
			baseMapper.insert(paperFileUpload);

		}

		return pdfFileList;

	}

	@Override
	public void updatePaperFile(Paper paper, MultipartFile[] files) {

		// 1.找到屬於這篇稿件的，有關ABSTRACTS_PDF 和 ABSTRACTS_DOCX的附件，
		List<PaperFileUpload> paperFileUploadList = this.getAbstractsByPaperId(paper.getPaperId());

		// 2.遍歷刪除舊的檔案
		for (PaperFileUpload paperFileUpload : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 刪除附件檔案的原本資料
			this.deletePaperFile(paperFileUpload.getPaperFileUploadId());

		}

		// 遍歷新增新的檔案
		for (MultipartFile file : files) {

			// 先定義 PaperFileUpload ,後續組裝使用
			PaperFileUpload paperFileUpload = new PaperFileUpload();
			paperFileUpload.setPaperId(paper.getPaperId());

			// 處理檔名和擴展名
			String originalFilename = file.getOriginalFilename();
			String fileExtension = minioUtil.getFileExtension(originalFilename);

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
				paperFileUpload.setType(PaperFileTypeEnum.ABSTRACTS_PDF.getValue());

			} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
				path += "/docx/";
				paperFileUpload.setType(PaperFileTypeEnum.ABSTRACTS_DOCX.getValue());
			}

			// 上傳檔案至Minio
			// 獲取回傳的檔案URL路徑,加上minioBucketName 準備組裝PaperFileUpload
			String uploadUrl = minioUtil.upload(minioBucketName, path, fileName, file);
			uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

			// 設定檔案路徑
			paperFileUpload.setPath(uploadUrl);

			// 放入資料庫
			baseMapper.insert(paperFileUpload);

		}

	}

	@Override
	public void deletePaperFile(Long paperFileUploadId) {
		baseMapper.deleteById(paperFileUploadId);
	}

	@Override
	public void deletePaperFileByPaperId(Long paperId) {
		// 1.找尋稿件的附件列表
		List<PaperFileUpload> paperFileUploadList = this.getPaperFileListByPaperId(paperId);

		// 2.遍歷並刪除檔案 及 資料庫數據
		for (PaperFileUpload paperFile : paperFileUploadList) {

			// 獲取檔案Path,但要移除/minioBuckerName/的這節
			// 這樣會只有單純的minio path
			String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFile.getPath());

			// 移除Minio中的檔案
			minioUtil.removeObject(minioBucketName, filePathInMinio);

			// 移除paperFileUpload table 中的資料
			this.deletePaperFile(paperFile.getPaperFileUploadId());

		}
	}

	@Override
	public void deletePaperFileUploadList(List<Long> paperFileUploadIds) {
		baseMapper.deleteBatchIds(paperFileUploadIds);
	}

	/** ----------- 第二階段 稿件附件 ------------- */

	@Override
	public List<PaperFileUpload> getSecondStagePaperFilesByPaperId(Long paperId) {
		LambdaQueryWrapper<PaperFileUpload> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperFileUpload::getPaperId, paperId)
				.eq(PaperFileUpload::getType, PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue());
		return baseMapper.selectList(queryWrapper);

	}

	@Override
	public ChunkResponseVO uploadSecondStagePaperFileChunk(Paper paper, AddSlideUploadDTO addSlideUploadDTO,
			MultipartFile file) {
		// 1.組裝合併後檔案的路徑, 目前在 稿件/第二階段/投稿類別/
		String mergedBasePath = SECOND_BASE_PATH + paper.getAbsType() + "/";

		// 2.上傳檔案分片
		ChunkResponseVO chunkResponseVO = sysChunkFileService.uploadChunk(file, mergedBasePath,
				addSlideUploadDTO.getChunkUploadDTO());

		// 3.當FilePath 不等於 null 時, 代表整個檔案都 merge 完成，具有可查看的Path路徑
		// 4.所以可以更新到paper 的附件表中，因為這個也是算在這篇稿件的
		if (chunkResponseVO.getFilePath() != null) {
			// 先定義 PaperFileUpload ,並填入paperId 後續組裝使用
			PaperFileUpload paperFileUpload = new PaperFileUpload();
			paperFileUpload.setPaperId(paper.getPaperId());
			// 設定檔案類型, 二階段都為supplementary_material 不管是poster、slide、video 都統一設定
			paperFileUpload.setType(PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue());
			// 設定檔案路徑，組裝 bucketName 和 Path 進資料庫當作真實路徑
			paperFileUpload.setPath("/" + minioBucketName + "/" + chunkResponseVO.getFilePath());
			// 設定檔案名稱
			paperFileUpload.setFileName(addSlideUploadDTO.getChunkUploadDTO().getFileName());
			// 放入資料庫
			baseMapper.insert(paperFileUpload);

		}

		return chunkResponseVO;

	}

	@Override
	public ChunkResponseVO updateSecondStagePaperFileChunk(Paper paper, PutSlideUploadDTO putSlideUploadDTO,
			MultipartFile file) {
		// 再靠paperUploadFileId , 查詢到已經上傳過一次的slide附件
		PaperFileUpload existPaperFileUpload = this.getById(putSlideUploadDTO.getPaperFileUploadId());

		//如果查不到，報錯
		if (existPaperFileUpload == null) {
			throw new PaperAbstractsException("No matching submissions attachment");
		}

		// 組裝合併後檔案的路徑, 目前在 稿件/第二階段/投稿類別/
		String mergedBasePath = SECOND_BASE_PATH + paper.getAbsType() + "/";

		ChunkResponseVO chunkResponseVO = sysChunkFileService.uploadChunk(file, mergedBasePath,
				putSlideUploadDTO.getChunkUploadDTO());

		// 當FilePath 不等於 null 時, 代表整個檔案都 merge 完成，具有可查看的Path路徑
		// 所以可以更新到paper 的附件表中，因為這個也是算在這篇稿件的
		if (chunkResponseVO.getFilePath() != null) {

			// 拿到舊的 PaperFileUpload 
			PaperFileUpload currentPaperFileUpload = this.getById(putSlideUploadDTO.getPaperFileUploadId());

			// 刪除舊檔案 和 DB 紀錄
			String oldFilePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName,
					currentPaperFileUpload.getPath());

			// 當檔名不一樣時要刪除舊檔案，檔名相同Minio會直接覆蓋
			if (!oldFilePathInMinio.equals(chunkResponseVO.getFilePath())) {
				minioUtil.removeObject(minioBucketName, oldFilePathInMinio);
				
				// 檔名不一樣時，刪除分片上傳紀錄，一樣則不要刪,避免sysChunk紀錄混亂
				sysChunkFileService.deleteSysChunkFileByPath(oldFilePathInMinio);
				
			}



			// 設定檔案路徑，組裝 bucketName 和 Path 進資料庫當作真實路徑
			currentPaperFileUpload.setPath("/" + minioBucketName + "/" + chunkResponseVO.getFilePath());
			// 設定檔案名稱
			currentPaperFileUpload.setFileName(putSlideUploadDTO.getChunkUploadDTO().getFileName());
			// 更新資料庫
			this.updateById(currentPaperFileUpload);

		}

		return chunkResponseVO;

	}

	@Override
	public void removeSecondStagePaperFile(Long paperId, Long paperFileUploadId) {
		// 1.獲取到這個檔案附件
		LambdaQueryWrapper<PaperFileUpload> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(PaperFileUpload::getPaperId, paperId)
				.eq(PaperFileUpload::getPaperFileUploadId, paperFileUploadId)
				.eq(PaperFileUpload::getType, PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue());

		PaperFileUpload paperFileUpload = baseMapper.selectOne(queryWrapper);

		// 2.獲取檔案Path,但要移除/minioBuckerName/的這節
		// 這樣會只有單純的minio path
		String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, paperFileUpload.getPath());

		// 移除Minio中的檔案 和 DB資料
		minioUtil.removeObject(minioBucketName, filePathInMinio);
		sysChunkFileService.deleteSysChunkFileByPath(filePathInMinio);

		// 3.在 DB 中刪除資料
		baseMapper.delete(queryWrapper);

	}

}
