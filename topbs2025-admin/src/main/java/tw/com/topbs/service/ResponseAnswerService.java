package tw.com.topbs.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;

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
	 * @param responseId
	 * @return
	 */
	List<ResponseAnswer> getAnswersByResponseId(Long responseId);
	
	/**
	 * 根據 responseId 查詢此次回覆表單的所有回答細項<br>
	 * 並產生以fieldId 為key 以ResponseAnswer 為value的Map
	 * 
	 * @param responseId 表單回覆ID
	 * @return
	 */
	Map<Long,ResponseAnswer> getAnswersKeyedByFieldId(Long responseId);
	
	/**
	 * 刪除跟 responseId 相符的所有 回覆細項
	 * 
	 * @param responseId
	 */
	void deleteAnswerByResponseId(Long responseId);
	
}
