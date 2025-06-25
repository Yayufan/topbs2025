package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperFileUploadConvert;
import tw.com.topbs.enums.PaperFileTypeEnum;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.mapper.PaperFileUploadMapper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
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

	private final PaperFileUploadConvert paperFileUploadConvert;
	private final SysChunkFileService sysChunkFileService;
	private final MinioUtil minioUtil;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	private String SECOND_BASE_PATH = "paper/second-stage/";

	@Override
	public PaperFileUpload getPaperFileUpload(Long paperFileUploadId) {
		PaperFileUpload paperFileUpload = baseMapper.selectById(paperFileUploadId);
		return paperFileUpload;
	}

	@Override
	public List<PaperFileUpload> getPaperFileUploadList() {
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(null);
		return paperFileUploadList;
	}

	@Override
	public List<PaperFileUpload> getPaperFileUploadListByPaperId(Long paperId) {
		// 找尋稿件的附件列表
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);
		return paperFileUploadList;
	}

	@Override
	public List<PaperFileUpload> getPaperFileUploadListByPaperIds(Collection<Long> paperIds) {

		if (paperIds.isEmpty()) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.in(PaperFileUpload::getPaperId, paperIds);
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);

		return paperFileUploadList;
	}

	@Override
	public Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdAtFirstReviewStage(Collection<Long> paperIds) {
		return this.getPaperFileUploadListByPaperIds(paperIds)
				.stream()
				.filter(paperFileUpload -> PaperFileTypeEnum.ABSTRACTS_PDF.getValue().equals(paperFileUpload.getType())
						|| PaperFileTypeEnum.ABSTRACTS_DOCX.getValue().equals(paperFileUpload.getType()))
				.collect(Collectors.groupingBy(PaperFileUpload::getPaperId, // 使用 paperId 作為 key
						Collectors.toList()));
	}

	@Override
	public Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdAtSecondReviewStage(Collection<Long> paperIds) {
		return this.getPaperFileUploadListByPaperIds(paperIds)
				.stream()
				.filter(paperFileUpload -> PaperFileTypeEnum.SUPPLEMENTARY_MATERIAL.getValue()
						.equals(paperFileUpload.getType()))
				.collect(Collectors.groupingBy(PaperFileUpload::getPaperId, // 使用 paperId 作為 key
						Collectors.toList()));
	}

	@Override
	public Map<Long, List<PaperFileUpload>> groupFileUploadsByPaperId(Collection<Long> paperIds) {
		return this.getPaperFileUploadListByPaperIds(paperIds)
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

		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);
		return paperFileUploadList;
	}

	@Override
	public IPage<PaperFileUpload> getPaperFileUploadPage(Page<PaperFileUpload> page) {
		Page<PaperFileUpload> paperFileUploadPage = baseMapper.selectPage(page, null);
		return paperFileUploadPage;
	}

	@Override
	public void addPaperFileUpload(AddPaperFileUploadDTO addPaperFileUploadDTO) {
		PaperFileUpload paperFileUpload = paperFileUploadConvert.addDTOToEntity(addPaperFileUploadDTO);
		baseMapper.insert(paperFileUpload);
		return;
	}

	@Override
	public void updatePaperFileUpload(PutPaperFileUploadDTO putPaperFileUploadDTO) {
		PaperFileUpload paperFileUpload = paperFileUploadConvert.putDTOToEntity(putPaperFileUploadDTO);
		baseMapper.updateById(paperFileUpload);

	}

	@Override
	public void deletePaperFileUpload(Long paperFileUploadId) {
		baseMapper.deleteById(paperFileUploadId);

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
			minioUtil.removeObject(minioBucketName, oldFilePathInMinio);
			sysChunkFileService.deleteSysChunkFileByPath(oldFilePathInMinio);

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
