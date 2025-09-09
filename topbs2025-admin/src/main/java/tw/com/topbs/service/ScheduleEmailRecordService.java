package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddScheduleEmailRecordDTO;
import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;

/**
 * <p>
 * 排程寄信任務的收信者,及信件內容 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
public interface ScheduleEmailRecordService extends IService<ScheduleEmailRecord> {

	/**
	 * 根據ID獲取排程寄信紀錄
	 * 
	 * @param id
	 * @return
	 */
	ScheduleEmailRecord getScheduleEmailRecord(Long id);

	/**
	 * 獲取全部排程寄信紀錄
	 * 
	 * @return
	 */
	List<ScheduleEmailRecord> getScheduleEmailRecordList();
	
	/**
	 * 根據 ScheduleEmailTaskId 拿到此任務要寄送的所有紀錄
	 * 
	 * @param ScheduleEmailTaskId
	 * @return
	 */
	List<ScheduleEmailRecord> getScheduleEmailRecordListByTaskId(Long ScheduleEmailTaskId);

	/**
	 * 獲取排程寄信紀錄(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<ScheduleEmailRecord> getScheduleEmailRecordPage(Page<ScheduleEmailRecord> page);

	/**
	 * 新增排程寄信紀錄
	 * 
	 * @param scheduleEmailRecord
	 * @return
	 */
	Long addScheduleEmailRecord(ScheduleEmailRecord scheduleEmailRecord);

	/**
	 * 根據ID 刪除 排程寄信紀錄
	 * 
	 * @param scheduleEmailRecordId
	 */
	void deleteScheduleEmailRecord(Long scheduleEmailRecordId);

}
