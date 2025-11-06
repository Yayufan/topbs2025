package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;

@Validated
public interface PaperService extends IService<Paper> {

	/**
	 * 獲取當前稿件總數
	 * 
	 * @return
	 */
	long getPaperCount();
	
	/**
	 * 根據稿件狀態，獲取符合此狀態的稿件總數
	 * 
	 * @param status
	 * @return
	 */
	long getPaperCountByStatus(Integer status);

	/**
	 * 拿到當前團體標籤的index
	 * 
	 * @param groupSize 一組的數量(人數)
	 * @return
	 */
	int getPaperGroupIndex(int groupSize);
	
	/**
	 * 根據稿件狀態，獲取符合此狀態 團體標籤的index
	 * 
	 * @param groupSize
	 * @param status
	 * @return
	 */
	int getPaperGroupIndexByStatus(int groupSize,Integer status);

	/**
	 * 給後台管理者，獲取單一稿件
	 * 
	 * @param paperId
	 * @return
	 */
	Paper getPaper(Long paperId);

	/**
	 * 校驗是否為稿件的擁有者
	 * 
	 * @param paperId
	 * @param memberId
	 */
	void validateOwner(Long paperId, Long memberId);
	
	/**
	 * 傳入paperId 和 memberId 查找特定 Paper
	 * 
	 * @param paperId
	 * @param memberId
	 * @return
	 */
	Paper getPaperByOwner(Long paperId, Long memberId);

	/**
	 * 給會員本身，獲取他所投稿的單一稿件
	 * 
	 * @param paperId
	 * @param memberId
	 * @return
	 */
	Paper getPaper(Long paperId, Long memberId);
	
	/**
	 * mybatis 原始高速查詢所有Paper<br>
	 * 輸出Excel數據適用
	 * @return
	 */
	List<Paper> getPapersEfficiently();

	List<Paper> getPaperListByIds(Collection<Long> paperIds);

	/**
	 * 給會員本身，獲取他所投稿的所有稿件
	 * 
	 * @param memberId
	 * @return
	 */
	List<Paper> getPaperListByMemberId(Long memberId);

	/** -------------- 以下為舊code ------------------ */

	/**
	 * 根據條件查詢 稿件 分頁對象
	 * 
	 * @param pageable
	 * @param queryText
	 * @param status
	 * @param absType
	 * @param absProp
	 * @return
	 */
	IPage<Paper> getPaperPageByQuery(Page<Paper> pageable, String queryText, Integer status, String absType,
			String absProp);


	/**
	 * 根據paperIds,獲取範圍內 paper的Map對象
	 * 
	 * @param paperIds
	 * @return 以paperId為key , 以Paper為value的Map對象
	 */
	Map<Long, Paper> getPaperMapById(Collection<Long> paperIds);

	/**
	 * 新增稿件資訊
	 * 
	 * @param addPaperDTO
	 * @return
	 */
	Paper addPaper(AddPaperDTO addPaperDTO);

	/**
	 * 給會員本身，修改稿件資訊
	 * 
	 * @param putPaperDTO
	 */
	Paper updatePaper(PutPaperDTO putPaperDTO);

	/**
	 * 給後台管理者，修改稿件審核狀態 及 公布發表編號、組別等
	 * 
	 * @param puPaperForAdminDTO
	 */
	void updatePaperForAdmin(PutPaperForAdminDTO puPaperForAdminDTO);

	/**
	 * 刪除單一稿件
	 * 
	 * @param paperId
	 */
	void deletePaper(Long paperId);

	/**
	 * 給後台管理者，批量刪除稿件
	 * 
	 * @param paperIds
	 */
	void deletePaperList(List<Long> paperIds);
	
	/**
	 * 校驗摘要檔案
	 * 
	 * @param files
	 */
	void validateAbstractsFiles(MultipartFile[] files);

	/** 以下為入選後，第二階段，上傳slide、poster、video */

	
	/**
	 * 查找 第二階段 檔案上傳的列表
	 * 
	 * @param paperId
	 * @param memberId
	 * @return
	 */
	List<PaperFileUpload> getSecondStagePaperFile(Long paperId, Long memberId);




}
