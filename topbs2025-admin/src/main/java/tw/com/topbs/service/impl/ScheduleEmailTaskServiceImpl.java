package tw.com.topbs.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.ScheduleEmailTaskConvert;
import tw.com.topbs.enums.ScheduleEmailStatus;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.mapper.ScheduleEmailTaskMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddScheduleEmailTaskDTO;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;
import tw.com.topbs.service.ScheduleEmailTaskService;

/**
 * <p>
 * 排程的電子郵件任務 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
@Service
@RequiredArgsConstructor
public class ScheduleEmailTaskServiceImpl extends ServiceImpl<ScheduleEmailTaskMapper, ScheduleEmailTask>
		implements ScheduleEmailTaskService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;
	private final ScheduleEmailTaskConvert scheduleEmailTaskConvert;

	@Override
	public ScheduleEmailTask getScheduleEmailTask(Long id) {
		ScheduleEmailTask scheduleEmailTask = baseMapper.selectById(id);
		return scheduleEmailTask;
	}

	@Override
	public List<ScheduleEmailTask> getScheduleEmailTaskList() {
		List<ScheduleEmailTask> scheduleEmailTaskList = baseMapper.selectList(null);
		return scheduleEmailTaskList;
	}

	@Override
	public IPage<ScheduleEmailTask> getScheduleEmailTaskPage(Page<ScheduleEmailTask> page) {
		Page<ScheduleEmailTask> scheduleEmailTaskPage = baseMapper.selectPage(page, null);
		return scheduleEmailTaskPage;
	}

	@Override
	public Long addScheduleEmailTask(AddScheduleEmailTaskDTO addScheduleEmailTaskDTO) {

		// 1.轉換數據
		ScheduleEmailTask scheduleEmailTask = scheduleEmailTaskConvert.addDTOToEntity(addScheduleEmailTaskDTO);

		// 2.截斷 任務時間 那天的開始時間
		LocalDateTime startOfDay = scheduleEmailTask.getStartTime().truncatedTo(ChronoUnit.DAYS); // 2025-08-07 00:00:00
		// 加一天再減1秒得到當天結束
		LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1); // 2025-08-07 23:59:59

		// 3.取出狀態為 pending 且執行日期與當前任務相同的資料
		LambdaQueryWrapper<ScheduleEmailTask> scheduleEmailTaskWrapper = new LambdaQueryWrapper<>();
		scheduleEmailTaskWrapper.eq(ScheduleEmailTask::getStatus, ScheduleEmailStatus.PENDING.getValue())
				.between(ScheduleEmailTask::getStartTime, startOfDay, endOfDay);
		List<ScheduleEmailTask> targetDayTaskList = baseMapper.selectList(scheduleEmailTaskWrapper);

		// 4.設置信件日額度為300，並且先減去此次目標信件數量
		int dailyEmailQuota = 300;
		dailyEmailQuota = dailyEmailQuota - scheduleEmailTask.getExpectedEmailVolume();

		// 5.之後再減去預計目標日期要執行的信件數量
		for (ScheduleEmailTask task : targetDayTaskList) {
			//減去目前已經有的
			dailyEmailQuota = dailyEmailQuota - task.getExpectedEmailVolume();
		}

		// 6.如果沒有信件額度返回異常，不進行信件任務排程
		if (dailyEmailQuota < 0) {
			throw new EmailException(
					"目標任務時間" + startOfDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "已經無足夠信件額度可以寄信，請更換時間");
		}

		baseMapper.insert(scheduleEmailTask);
		return scheduleEmailTask.getScheduleEmailTaskId();
	}

	@Override
	public void deleteScheduleEmailTask(Long scheduleEmailTaskId) {
		baseMapper.deleteById(scheduleEmailTaskId);
	}

}
