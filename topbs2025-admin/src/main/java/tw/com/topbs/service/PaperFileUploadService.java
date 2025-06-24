package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;

public interface PaperFileUploadService extends IService<PaperFileUpload> {

	PaperFileUpload getPaperFileUpload(Long paperFileUploadId);

	List<PaperFileUpload> getPaperFileUploadList();

	/**
	 * 根據paperId 找到對應的稿件的，投稿附件
	 * 
	 * @param paperId
	 * @return
	 */
	List<PaperFileUpload> getPaperFileUploadListByPaperId(Long paperId);

	/**
	 * 根據 paperIds 找到對應複數稿件的，投稿附件
	 * 
	 * @param paperIds
	 * @return
	 */
	List<PaperFileUpload> getPaperFileUploadListByPaperIds(Collection<Long> paperIds);

	/**
	 * 根據paperId分組返回 搜尋第一階段投稿的附件(摘要)
	 * 
	 * @param paperIds
	 * @return paperId為key，第一階段檔案列表(摘要) 為值的Map
	 */
	Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdAtFirstReviewStage(Collection<Long> paperIds);

	/**
	 * 根據paperId分組返回 搜尋第二階段投稿的 所有附件(附加資料)
	 * 
	 * @param paperIds
	 * @return paperId為key，第二階段檔案列表(附加檔案) 為值的Map
	 */
	Map<Long, List<PaperFileUpload>> getPaperFileMapByPaperIdAtSecondReviewStage(Collection<Long> paperIds);

	/**
	 * 根據paperId分組返回 稿件附件列表
	 * 
	 * @param paperIds 稿件列表
	 * @return paperId為鍵 paperFileUpload 為值的Map
	 */
	Map<Long, List<PaperFileUpload>> groupFileUploadsByPaperId(Collection<Long> paperIds);

	/**
	 * 根據paperId 在投稿附件列表中找到 word 和 pdf的檔案，
	 * 這兩個通常是摘要的檔案格式
	 * 
	 * @param paperId
	 * @return
	 */
	List<PaperFileUpload> getAbstractsByPaperId(Long paperId);

	IPage<PaperFileUpload> getPaperFileUploadPage(Page<PaperFileUpload> page);

	void addPaperFileUpload(AddPaperFileUploadDTO addPaperFileUploadDTO);

	void updatePaperFileUpload(PutPaperFileUploadDTO putPaperFileUploadDTO);

	void deletePaperFileUpload(Long paperFileUploadId);

	void deletePaperFileUploadList(List<Long> paperFileUploadIds);

	/** --------- 第二階段 稿件檔案----------- */

	/**
	 * 根據paperId 找到 第二階段 對應稿件的，投稿附件
	 * 
	 * @param paperId
	 * @return
	 */
	List<PaperFileUpload> getSecondStagePaperFilesByPaperId(Long paperId);

	/**
	 * 初次上傳附件 第二階段 稿件附件分片
	 * 
	 * @param paper
	 * @param addSlideUploadDTO
	 * @param file
	 * @return
	 */
	ChunkResponseVO uploadSecondStagePaperFileChunk(Paper paper, AddSlideUploadDTO addSlideUploadDTO,
			MultipartFile file);

	/**
	 * 更新附件檔案 第二階段 稿件附件分片
	 * 
	 * @param paper
	 * @param putSlideUploadDTO
	 * @param file
	 * @return
	 */
	ChunkResponseVO updateSecondStagePaperFileChunk(Paper paper, PutSlideUploadDTO putSlideUploadDTO,
			MultipartFile file);

	/**
	 * 根據paperId、paperFileUploadId 刪除 第二階段 稿件附件
	 * 
	 * @param paperId
	 * @param paperFileUploadId
	 */
	void removeSecondStagePaperFile(Long paperId, Long paperFileUploadId);

}
