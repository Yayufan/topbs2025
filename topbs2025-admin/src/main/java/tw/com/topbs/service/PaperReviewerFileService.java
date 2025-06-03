package tw.com.topbs.service;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerFileDTO;
import tw.com.topbs.pojo.entity.PaperReviewerFile;

/**
 * <p>
 * 給審稿委員的公文檔案和額外]資料 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-06-03
 */
public interface PaperReviewerFileService extends IService<PaperReviewerFile> {

	void addPaperReviewerFile(MultipartFile file, Long paperReviewerId);

	void updatePaperReviewerFile(MultipartFile file, PutPaperReviewerFileDTO putPaperReviewerFileDTO);

	void deletePaperReviewerFile(Long paperFileUploadId);

}
