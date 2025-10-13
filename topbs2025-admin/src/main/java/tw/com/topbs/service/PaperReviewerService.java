package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.dev33.satoken.stp.SaTokenInfo;
import tw.com.topbs.pojo.DTO.PaperReviewerLoginInfo;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;

public interface PaperReviewerService extends IService<PaperReviewer> {

	/**
	 * 獲取審稿者總數
	 * 
	 * @return
	 */
	long getReviewerCount();
	
	/**
	 * 獲取審稿者群組index
	 * 
	 * @param groupSize
	 * @return
	 */
	int getReviewerGroupIndex(int groupSize);
	
	/**
	 * 根據主鍵,拿到審稿者
	 * 
	 * @param reviewerId
	 * @return
	 */
	PaperReviewer getReviewerById(Long reviewerId);
	
	/**
	 * mybatis 原始高速查詢所有Reviewer<br>
	 * 輸出Excel數據適用
	 * @return
	 */
	List<PaperReviewer> getReviewersEfficiently();

	/**
	 * 根據reviewerIds拿到 符合範圍的 列表
	 * 
	 * @param reviewerIds
	 * @return
	 */
	List<PaperReviewer> getReviewerListByIds(Collection<Long> reviewerIds);
	
	/**
	 * 查詢符合能審核稿件類別的評審
	 * 
	 * @param absType
	 * @return
	 */
	List<PaperReviewer> getReviewerListByAbsType(String absType);
	
	/**
	 * 查詢審稿者 分頁對象
	 * 
	 * @param page
	 * @return
	 */
	IPage<PaperReviewer> getReviewerPage(Page<PaperReviewer> page);
	
	/**
	 * 拿到 主鍵 和 對象 的映射對象
	 * 
	 * @param reviewerIds
	 * @return 以reviewerId為key , 以PaperReviewer為value的映射對象
	 */
	Map<Long,PaperReviewer> getReviewerMapById(Collection<Long> reviewerIds);

	/**
	 * 新增審稿委員
	 * 
	 * @param addPaperReviewerDTO
	 */
	void addPaperReviewer(AddPaperReviewerDTO addPaperReviewerDTO);

	/**
	 * 修改審稿委員
	 * 
	 * @param putPaperReviewerDTO
	 */
	void updatePaperReviewer(PutPaperReviewerDTO putPaperReviewerDTO);

	/**
	 * 刪除審稿委員
	 * 
	 * @param paperReviewerId
	 */
	void deletePaperReviewer(Long paperReviewerId);

	/**
	 * 批量刪除審稿委員
	 * 
	 * @param paperReviewerIds
	 */
	void deletePaperReviewerList(List<Long> paperReviewerIds);


	/** 以下為審稿委員自行使用的API */

	/**
	 * 審稿委員登入
	 * 
	 * @param paperReviewerLoginInfo
	 * @return
	 */
	SaTokenInfo login(PaperReviewerLoginInfo paperReviewerLoginInfo);

	/**
	 * 審稿委員登出
	 * 
	 */
	void logout();

	/**
	 * 透過token從緩存中取得資料
	 * 
	 * @return
	 */
	PaperReviewer getPaperReviewerInfo();


}
