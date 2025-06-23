package tw.com.topbs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;

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
	 * 給會員本身，獲取他所投稿的所有稿件
	 * 
	 * @param memberId
	 * @return
	 */
	List<PaperVO> getPaperList(Long memberId);

	/**
	 * 給後台管理者，獲取所有稿件(分頁)
	 * 
	 * @param pageable 分頁資訊
	 * @return
	 */
	IPage<PaperVO> getPaperPage(Page<Paper> pageable);

	/**
	 * 給後台管理者，多條件查詢，獲取所有稿件(分頁)
	 * 
	 * @param pageable  分頁資訊
	 * @param queryText 細部查詢文字，可配對全部作者、通訊作者email和電話、稿件標題、發表編號和群組
	 * @param status    審核狀態
	 * @param absType   投稿類別
	 * @return
	 */
	IPage<PaperVO> getPaperPage(Page<Paper> pageable, String queryText, Integer status, String absType, String absProp);

	/**
	 * 給會員本身，新增稿件
	 * 
	 * @param files
	 * @param addPaperDTO
	 * @throws IOException
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
	 * 給後台管理者，修改稿件審核狀態 及 公布發表編號、組別等
	 * 
	 * @param puPaperForAdminDTO
	 */
	void updatePaperForAdmin(PutPaperForAdminDTO puPaperForAdminDTO);

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

	/**
	 * 下載對應審核階段的稿件評分
	 * 
	 * @param response
	 * @param reviewStage 審核階段
	 * @throws UnsupportedEncodingException 
	 * @throws IOException 
	 */
	void downloadScoreExcel(HttpServletResponse response, String reviewStage) throws UnsupportedEncodingException, IOException;

	/**
	 * 為用戶新增/更新/刪除 複數審稿委員
	 * 
	 * @param reviewStage               審核階段
	 * @param targetPaperReviewerIdList
	 * @param paperId
	 */
	void assignPaperReviewerToPaper(String reviewStage, List<Long> targetPaperReviewerIdList, Long paperId);

	/**
	 * 只要審稿委員符合稿件類型，且沒有相同審核階段的記錄，就自動進行分配
	 * 
	 * @param reviewStage
	 */
	void autoAssignPaperReviewer(String reviewStage);

	/**
	 * 為 稿件 新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param paperId
	 */
	void assignTagToPaper(List<Long> targetTagIdList, Long paperId);

	/**
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的Papers 這邊指通訊作者
	 * 如果沒有傳任何tag則是寄給所有Paper
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void sendEmailToPapers(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/** 以下為入選後，第二階段，上傳slide、poster、video */

	/**
	 * 初次上傳slide，大檔案切割成分片，最後重新組裝
	 * 
	 * @param addSlideUploadDTO 稿件ID和分片資訊
	 * @param memberId          會員ID
	 * @param file
	 * @return
	 */
	ChunkResponseVO uploadSlideChunk(AddSlideUploadDTO addSlideUploadDTO, Long memberId, MultipartFile file);

	/**
	 * 更新slide，大檔案切割成分片，最後重新組裝
	 * 
	 * @param putSlideUploadDTO 稿件ID、稿件附件ID和分片資訊
	 * @param memberId          會員ID
	 * @param file              檔案分片
	 * @return
	 */
	ChunkResponseVO updateSlideChunk(PutSlideUploadDTO putSlideUploadDTO, Long memberId, MultipartFile file);

}
