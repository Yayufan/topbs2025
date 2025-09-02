package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddScheduleEmailTaskDTO;
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
	 * @param id
	 * @return
	 */
	ScheduleEmailTask getScheduleEmailTask(Long id);

	/**
	 * 獲取全部排程信件任務
	 * @return
	 */
	List<ScheduleEmailTask> getScheduleEmailTaskList();

	/**
	 * 獲取排程信件任務(分頁)
	 * @param page
	 * @return
	 */
	IPage<ScheduleEmailTask> getScheduleEmailTaskPage(Page<ScheduleEmailTask> page);

	/**
	 * 新增排程信件任務
	 * @param scheduleEmailTask
	 * @return
	 */
	Long addScheduleEmailTask(ScheduleEmailTask scheduleEmailTask);

	/**
	 * 刪除排程信件任務
	 * @param scheduleEmailTaskId
	 */
	void deleteScheduleEmailTask(Long scheduleEmailTaskId);
	
}
