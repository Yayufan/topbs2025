package tw.com.topbs.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;

/**
 * <p>
 * 排程的電子郵件任務 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
public interface ScheduleEmailTaskService extends IService<ScheduleEmailTask> {

	/**
	 * 根據ID獲取排程信件任務
	 * 
	 * @param id
	 * @return
	 */
	ScheduleEmailTask getScheduleEmailTask(Long id);

	/**
	 * 獲取全部排程信件任務
	 * 
	 * @return
	 */
	List<ScheduleEmailTask> getScheduleEmailTaskList();

	/**
	 * 獲取本日尚未執行的排程任務信件量
	 * 
	 * @param today
	 * @return
	 */
	int getPendingExpectedEmailVolumeByToday();
	
	/**
	 * 處理到期任務
	 * @return
	 */
	List<ScheduleEmailTask> getProcessDueTasks();
	
	
	/**
	 * 獲取排程信件任務(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<ScheduleEmailTask> getScheduleEmailTaskPage(Page<ScheduleEmailTask> page);

	/**
	 * 排程任務設置
	 * 
	 * @param <T>
	 * @param sendEmailDTO      email 信息
	 * @param recipients        這次的收件者列表
	 * @param recipientCategory 收件者類別
	 * @param emailExtractor    從收件者列表獲取email的Function
	 * @param contentReplacer   content 替換 merge Tag的Function
	 * 
	 */
	<T> void processScheduleEmailTask(SendEmailDTO sendEmailDTO, List<T> recipients, String recipientCategory,
			Function<T, String> emailExtractor, BiFunction<String, T, String> contentReplacer);

	/**
	 * 排程任務設置，帶附件的版本
	 * 
	 * @param <T>
	 * @param sendEmailDTO           email 信息
	 * @param recipients             這次的收件者列表
	 * @param recipientCategory      收件者類別
	 * @param emailExtractor         從收件者列表獲取email的Function
	 * @param contentReplacer        content 替換 merge Tag的Function
	 * @param attachmentPathProvider 寄送附件的路徑列表獲取的Function
	 */
	<T> void processScheduleEmailTask(SendEmailDTO sendEmailDTO, List<T> recipients, String recipientCategory,
			Function<T, String> emailExtractor, BiFunction<String, T, String> contentReplacer,
			Function<T, List<String>> attachmentPathProvider);

	
	/**
	 * 新增排程信件任務
	 * 
	 * @param scheduleEmailTask
	 * @return
	 */
	Long addScheduleEmailTask(ScheduleEmailTask scheduleEmailTask);

	/**
	 * 刪除排程信件任務
	 * 
	 * @param scheduleEmailTaskId
	 */
	void deleteScheduleEmailTask(Long scheduleEmailTaskId);
	
	/**
	 * 根據 scheduleEmailTaskId 拿到他要寄送的所有資料
	 * 
	 * @param scheduleEmailTaskId
	 * @return
	 */
	List<ScheduleEmailRecord> getTaskRecordsBytaskId(Long scheduleEmailTaskId);

}
