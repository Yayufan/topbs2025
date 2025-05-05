package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
import tw.com.topbs.pojo.entity.PaperFileUpload;

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

}
