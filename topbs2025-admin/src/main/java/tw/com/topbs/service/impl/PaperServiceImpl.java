package tw.com.topbs.service.impl;

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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.constant.I18nMessageKey;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.exception.PaperAbstractsException;
import tw.com.topbs.helper.MessageHelper;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private final MessageHelper messageHelper;
	private final PaperConvert paperConvert;
	private final PaperFileUploadService paperFileUploadService;

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
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}
	}

	@Override
	public Paper getPaper(Long paperId, Long memberId) {
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId).eq(Paper::getPaperId, paperId);

		// 如果查無資訊直接報錯
		Paper paper = baseMapper.selectOne(paperQueryWrapper);
		if (paper == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}
		return paper;

	}

	@Override
	public List<Paper> getPapersEfficiently() {
		return baseMapper.selectPapers();
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
	public Map<Long, Paper> getPaperMapById(Collection<Long> paperIds) {
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
		this.validateOwner(putPaperDTO.getPaperId(), putPaperDTO.getMemberId());

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
					throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.Attachment.FILE_SIZE));
				}

				// 檢查檔案類型
				String contentType = file.getContentType();
				if (!"application/pdf".equals(contentType) && !"application/msword".equals(contentType)
						&& !"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
								.equals(contentType)) {
					throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.Attachment.FILE_TYPE));
				}

			}
		}

	}

	/** 以下為入選後，第二階段，查看/上傳/更新 slide、poster、video */

	@Override
	public List<PaperFileUpload> getSecondStagePaperFile(Long paperId, Long memberId) {
		// 1.找到memberId 和 paperId 都符合的唯一數據，memberId是避免他去搜尋到別人的數據
		Paper paper = this.getPaperByOwner(paperId, memberId);

		// 如果查無資訊直接報錯
		if (paper == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}

		// 2.查找此稿件 第二階段 的附件檔案
		return paperFileUploadService.getSecondStagePaperFilesByPaperId(paperId);

	}

	@Override
	public ChunkResponseVO uploadSlideChunk(AddSlideUploadDTO addSlideUploadDTO, Long memberId, MultipartFile file) {

		// 1.透過paperId 和 memberId 找到特定稿件
		Paper paper = this.getPaperByOwner(addSlideUploadDTO.getPaperId(), memberId);

		if (paper == null) {
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
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
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
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
			throw new PaperAbstractsException(messageHelper.get(I18nMessageKey.Paper.NO_MATCH));
		}

		// 2.透過paperFileUploadId 刪除第二階段檔案 (DB 和 Minio)
		paperFileUploadService.removeSecondStagePaperFile(paperId, paperFileUploadId);

	}

}
