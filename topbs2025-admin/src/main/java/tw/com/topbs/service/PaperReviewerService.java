package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;

public interface PaperReviewerService extends IService<PaperReviewer> {

	PaperReviewer getPaperReviewer(Long paperReviewerId);
	
	List<PaperReviewer> getPaperReviewerList();
	
	IPage<PaperReviewer> getPaperReviewerPage(Page<PaperReviewer> page);
	
	void addPaperReviewer(AddPaperReviewerDTO addPaperReviewerDTO);
	
	void updatePaperReviewer(PutPaperReviewerDTO putPaperReviewerDTO);
	
	void deletePaperReviewer(Long paperReviewerId);
	
	void deletePaperReviewerList(List<Long> paperReviewerIds);
	
}
