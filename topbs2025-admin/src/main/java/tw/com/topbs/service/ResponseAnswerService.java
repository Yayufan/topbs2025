package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.BO.ResponseAnswerMatrixBO;
import tw.com.topbs.pojo.entity.ResponseAnswer;

/**
 * <p>
 * 表單回覆內容 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface ResponseAnswerService extends IService<ResponseAnswer> {

	/**
	 * 根據 responseId 查詢此次回覆表單的所有回答細項
	 * 
	 * @param responseId 表單回覆ID
	 * @return
	 */
	List<ResponseAnswer> searchAnswersByResponse(Long responseId);
	
	/**
	 * 根據responseIds 獲取符合的回覆表單 所有回答細項
	 * 
	 * @param responseIds
	 * @return
	 */
	List<ResponseAnswer> searchAnswersByResponses(Collection<Long> responseIds);
	
	
	/**
	 * 根據 responseId 查詢此次回覆表單的所有回答細項<br>
	 * 並產生以fieldId 為key 以ResponseAnswer 為value的Map
	 * 
	 * @param responseId 表單回覆ID
	 * @return
	 */
	Map<Long,ResponseAnswer> searchAnswerMapByFieldId(Long responseId);
	
	
	/**
	 * 根據 responseIds 查詢此表單所有回覆的回答細項<br>
	 * 並產生以responseId分組 , 裡面為 以fieldId為key 以 answer為value的 map
	 * 
	 * @param responseIds
	 * @return
	 */
	ResponseAnswerMatrixBO searchResponseAnswerMatrixBO(Collection<Long> responseIds);
	
	/**
	 * 刪除跟 responseId 相符的所有 回覆細項
	 * 
	 * @param responseId
	 */
	void removeByResponse(Long responseId);
	
	/**
	 * 刪除跟 responseIds 相符的所有 回覆細項
	 * 
	 * @param responseIds
	 */
	void removeByResponses(Collection<Long> responseIds);
	
}
