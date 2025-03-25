package tw.com.topbs.service;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.validation.Valid;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;

@Validated
public interface PaperService extends IService<Paper> {

	/**
	 * 給後台管理者，獲取單一稿件
	 * 
	 * @param paperId
	 * @return
	 */
	PaperVO getPaper(Long paperId);

	/**
	 * 給會員本身，獲取他所投稿的單一稿件
	 * 
	 * @param paperId
	 * @param memberId
	 * @return
	 */
	PaperVO getPaper(Long paperId, Long memberId);

	/**
	 * 給後台管理者，獲取所有稿件
	 * 
	 * @return
	 */
	List<Paper> getPaperList();

	/**
	 * 給會員本身，獲取他所投稿的所有稿件
	 * 
	 * @param memberId
	 * @return
	 */
	List<PaperVO> getPaperList(Long memberId);

	/**
	 * 給後台管理者，獲取所有稿件(分頁)
	 * 
	 * @param pageable
	 * @return
	 */
	IPage<PaperVO> getPaperPage(Page<Paper> pageable);

	/**
	 * 給會員本身，新增稿件
	 * 
	 * @param files
	 * @param addPaperDTO
	 */
	void addPaper(MultipartFile[] files, @Valid AddPaperDTO addPaperDTO);

	/**
	 * 給會員本身，修改稿件
	 * 
	 * @param files
	 * @param putPaperDTO
	 */
	void updatePaper(MultipartFile[] files, @Valid PutPaperDTO putPaperDTO);

	/**
	 * 給後台管理者，刪除單一稿件
	 * 
	 * @param paperId
	 */
	void deletePaper(Long paperId);

	/**
	 * 給會員本身，刪除他所投稿的單一稿件
	 * 
	 * @param paperId
	 * @param memberId
	 */
	void deletePaper(Long paperId, Long memberId);

	/**
	 * 給後台管理者，批量刪除稿件
	 * 
	 * @param paperIds
	 */
	void deletePaperList(List<Long> paperIds);

}
