package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.constants.I18nMessageKey;
import tw.com.topbs.constants.PaperFileConstants;
import tw.com.topbs.enums.PaperFileTypeEnum;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.helper.MessageHelper;
import tw.com.topbs.mapper.PaperFileUploadMapper;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.service.SysChunkFileService;
import tw.com.topbs.utils.S3Util;

@Service
@RequiredArgsConstructor
public class PaperFileUploadServiceImpl extends ServiceImpl<PaperFileUploadMapper, PaperFileUpload>
		implements PaperFileUploadService {

	// Redisson Keys 儲存 paperFileId
	private static final String PAPER_FILE_KEY_PREFIX = "paper:file:";

	private final MessageHelper messageHelper;
	private final SysChunkFileService sysChunkFileService;
	private final S3Util s3Util;

	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

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
			String fileExtension = s3Util.getFileExtension(originalFilename);

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
			String fileName = paper.getAbsType() + "_" + paper.getFirstAuthor() + fileExtension;

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

			// 上傳檔案至S3,
			String dbUrl = s3Util.upload(path, fileName, file);

			// 設定檔案路徑
			paperFileUpload.setPath(dbUrl);

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

			// 獲取檔案Path,並透過S3Util提取S3Key
			String s3Key = s3Util.extractS3PathInDbUrl(bucketName, paperFileUpload.getPath());

			// 移除S3中的檔案
			s3Util.removeFile(bucketName, s3Key);

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
			String fileExtension = s3Util.getFileExtension(originalFilename);

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
			String fileName = paper.getAbsType() + "_" + paper.getFirstAuthor() + fileExtension;

			// 判斷是PDF檔 還是 DOCX檔 會變更path
			if (fileExtension.equals("pdf")) {
				path += "/pdf/";
				paperFileUpload.setType(PaperFileTypeEnum.ABSTRACTS_PDF.getValue());

			} else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
				path += "/docx/";
				paperFileUpload.setType(PaperFileTypeEnum.ABSTRACTS_DOCX.getValue());
			}

			// 上傳檔案至S3 , 拿到與bucket組合後的儲存路徑
			String dbUrl = s3Util.upload(path, fileName, file);

			// 設定檔案路徑
			paperFileUpload.setPath(dbUrl);

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

			// 獲取檔案Path,但要移除/bucker/的這節
			String s3Key = s3Util.extractS3PathInDbUrl(bucketName, paperFile.getPath());

			// 移除S3中的檔案
			s3Util.removeFile(bucketName, s3Key);

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
		String mergedBasePath = PaperFileConstants.SLIDE_BASE_PATH + "/" + paper.getAbsType() + "/";

		// 2.上傳檔案分片
		ChunkResponseVO chunkResponseVO = sysChunkFileService.uploadChunkS3(file, mergedBasePath,
				addSlideUploadDTO.getChunkUploadDTO());

		// 3.當FilePath 不等於 null 時, 代表整個檔案都 merge 完成，具有可查看的Path路徑
		// 4.所以更新到paper 的附件表中，因為這個也是算在這篇稿件的，但是可能會有競態產生
		String filePath = chunkResponseVO.getFilePath();
		if (filePath != null) {

			String fileName = addSlideUploadDTO.getChunkUploadDTO().getFileName();
			String sha256 = chunkResponseVO.getCurrentFileSha256();
			String paperFileKey = PAPER_FILE_KEY_PREFIX + sha256;
			RBucket<String> bucket = redissonClient.getBucket(paperFileKey);

			// 4-1. 先查 Redis
			String existingId = bucket.get();
			if (existingId != null) {
				return chunkResponseVO;
			}

			// 4-2. Redis 沒命中，先查 DB
			LambdaQueryWrapper<PaperFileUpload> queryWrapper = new LambdaQueryWrapper<>();
			queryWrapper.eq(PaperFileUpload::getPaperId, paper.getPaperId())
					.eq(PaperFileUpload::getPath, "/" + bucketName + "/" + filePath)
					.eq(PaperFileUpload::getFileName, fileName);
			PaperFileUpload existFile = baseMapper.selectOne(queryWrapper);

			if (existFile != null) {
				// DB 已存在，更新 Redis
				bucket.set(existFile.getPaperFileUploadId().toString(), 30, TimeUnit.SECONDS);
				return chunkResponseVO;
			}

			// 4-3. DB 也沒有，獲取鎖並進行插入
			RLock lock = redissonClient.getLock("db-insert-lock:" + sha256);
			boolean isLock = false;
			try {
				isLock = lock.tryLock(10, 30, TimeUnit.SECONDS);
				if (isLock) {
					// 4-3-1. 鎖內再次競態檢查
					existingId = bucket.get();
					if (existingId != null) {
						return chunkResponseVO;
					}

					//  4-3-2. 插入 DB
					PaperFileUpload paperFileUpload = new PaperFileUpload();
					paperFileUpload.setPaperId(paper.getPaperId());
					paperFileUpload.setType(PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue());
					paperFileUpload.setPath("/" + bucketName + "/" + filePath);
					paperFileUpload.setFileName(fileName);
					baseMapper.insert(paperFileUpload);

					//  4-3-3. 更新 Redis
					bucket.set(paperFileUpload.getPaperFileUploadId().toString(), 30, TimeUnit.SECONDS);
				}

			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
			} finally {
				if (isLock) {
					lock.unlock();
				}
			}
		}

		return chunkResponseVO;

	}

	@Override
	public ChunkResponseVO updateSecondStagePaperFileChunk(Paper paper, PutSlideUploadDTO putSlideUploadDTO,
			MultipartFile file) {

		// 1.再靠paperUploadFileId , 查詢到已經上傳過一次的slide附件
		PaperFileUpload existPaperFileUpload = this.getById(putSlideUploadDTO.getPaperFileUploadId());

		// 2.如果查不到，報錯
		if (existPaperFileUpload == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.Attachment.NO_MATCH));
		}

		// 3.組裝合併後檔案的路徑, 目前在 稿件/第二階段/投稿類別/
		String mergedBasePath = PaperFileConstants.SLIDE_BASE_PATH + "/" + paper.getAbsType() + "/";

		// 4.上傳分片
		ChunkResponseVO chunkResponseVO = sysChunkFileService.uploadChunkS3(file, mergedBasePath,
				putSlideUploadDTO.getChunkUploadDTO());

		// 5.當FilePath 不等於 null 時, 代表整個檔案都 merge 完成，具有可查看的Path路徑
		// 所以可以更新到paper 的附件表中，因為這個也是算在這篇稿件的
		if (chunkResponseVO.getFilePath() != null) {

			// 拿到舊的 PaperFileUpload 
			PaperFileUpload currentPaperFileUpload = this.getById(putSlideUploadDTO.getPaperFileUploadId());

			// 刪除舊檔案 和 DB 紀錄
			String oldS3Key = s3Util.extractS3PathInDbUrl(bucketName, currentPaperFileUpload.getPath());

			// 當檔名不一樣時要刪除舊檔案，檔名相同S3會直接覆蓋
			if (!oldS3Key.equals(chunkResponseVO.getFilePath())) {
				s3Util.removeFile(bucketName, oldS3Key);

				// 檔名不一樣時，刪除分片上傳紀錄，一樣則不要刪,避免sysChunk紀錄混亂
				sysChunkFileService.deleteSysChunkFileByPath(oldS3Key);

			}

			// 設定檔案路徑，組裝 bucketName 和 Path 進資料庫當作真實路徑
			currentPaperFileUpload.setPath("/" + bucketName + "/" + chunkResponseVO.getFilePath());
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

		// 2.獲取檔案Path,但要移除/bucker/的這節
		String s3Key = s3Util.extractS3PathInDbUrl(bucketName, paperFileUpload.getPath());

		// 移除S3中的檔案 和 DB資料
		s3Util.removeFile(bucketName, s3Key);
		sysChunkFileService.deleteSysChunkFileByPath(s3Key);

		// 3.在 DB 中刪除資料
		baseMapper.delete(queryWrapper);

	}



}
