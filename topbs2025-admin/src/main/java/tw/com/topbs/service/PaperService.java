package tw.com.topbs.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;

public interface PaperService extends IService<Paper> {

	Paper getPaper(Long paperId);
	
	List<Paper> getPaperList();
	
	IPage<Paper> getPaperPage(Page<Paper> pageable);
	
	void addPaper(MultipartFile[] files,AddPaperDTO addPaperDTO);
	
	void updatePaper(PutPaperDTO putPaperDTO);
	
	void deletePaper(Long paperId);
	
	void deletePaperList(List<Long> paperIds);
	
}
