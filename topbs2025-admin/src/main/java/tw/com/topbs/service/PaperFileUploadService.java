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
	
	IPage<PaperFileUpload> getPaperFileUploadPage(Page<PaperFileUpload> page);
	
	void addPaperFileUpload(AddPaperFileUploadDTO addPaperFileUploadDTO);
	
	void updatePaperFileUpload(PutPaperFileUploadDTO putPaperFileUploadDTO);
	
	void deletePaperFileUpload(Long paperFileUploadId);
	
	void deletePaperFileUploadList(List<Long> paperFileUploadIds);
	
}
