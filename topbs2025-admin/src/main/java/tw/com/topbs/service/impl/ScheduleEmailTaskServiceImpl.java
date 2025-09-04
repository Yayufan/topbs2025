package tw.com.topbs.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.redisson.api.RAtomicLong;
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
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;
import tw.com.topbs.service.ScheduleEmailRecordService;
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
	private final ScheduleEmailRecordService scheduleEmailRecordService;

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
	public int getPendingExpectedEmailVolumeByToday() {

		// 1.拿取今日的時間
		LocalDateTime today = LocalDateTime.now();

		// 2.將今日時間當作參數,拿到今日所有Pending任務
		List<ScheduleEmailTask> targetDayTaskList = this.getPendingEmailTaskListByDate(today);

		// 3.預設pending任務的寄信量為0
		int pendingExpectedEmailVolume = 0;

		// 4.循環加總已有任務信件量
		for (ScheduleEmailTask task : targetDayTaskList) {
			pendingExpectedEmailVolume = pendingExpectedEmailVolume + task.getExpectedEmailVolume();
		}

		// 5.返回今日預計要寄出的信件量
		return pendingExpectedEmailVolume;
	}

	@Override
	public IPage<ScheduleEmailTask> getScheduleEmailTaskPage(Page<ScheduleEmailTask> page) {
		Page<ScheduleEmailTask> scheduleEmailTaskPage = baseMapper.selectPage(page, null);
		return scheduleEmailTaskPage;
	}

	@Override
	public Long addScheduleEmailTask(ScheduleEmailTask scheduleEmailTask) {

		// 1.獲取DB中在這個時間內其他Pending 的任務
		List<ScheduleEmailTask> pendingEmailTaskList = this
				.getPendingEmailTaskListByDate(scheduleEmailTask.getStartTime());

		// 2.信件日額度為300，如果排程日期是今天,則要改為使用今日剩餘額
		long dailyEmailQuota = 300L;
		if (LocalDate.now().isEqual(scheduleEmailTask.getStartTime().toLocalDate())) {
			//從Redis中查看本日信件餘額
			RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
			dailyEmailQuota = quota.get();
		}

		System.out.println("剩餘額度為 " + dailyEmailQuota);

		dailyEmailQuota = dailyEmailQuota - scheduleEmailTask.getExpectedEmailVolume();

		// 3.之後再減去預計目標日期，要執行的信件數量
		for (ScheduleEmailTask task : pendingEmailTaskList) {
			//減去目前已經有的
			dailyEmailQuota = dailyEmailQuota - task.getExpectedEmailVolume();
		}

		// 4.如果沒有信件額度返回異常，不進行信件任務排程
		if (dailyEmailQuota < 0) {
			throw new EmailException(
					"目標任務時間" + scheduleEmailTask.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
							+ "已經無足夠信件額度可以寄信，請更換時間");
		}

		// 5.將任務新增進DB
		baseMapper.insert(scheduleEmailTask);
		return scheduleEmailTask.getScheduleEmailTaskId();
	}

	public <T> void processScheduleEmailTask(SendEmailDTO sendEmailDTO, List<T> recipients, String recipientCategory,
			Function<T, String> emailExtractor, BiFunction<String, T, String> contentReplacer) {

		// 1. 建立排程任務
		ScheduleEmailTask scheduleEmailTask = scheduleEmailTaskConvert.DTOToEntity(sendEmailDTO);
		scheduleEmailTask.setExpectedEmailVolume(recipients.size());
		scheduleEmailTask.setRecipientCategory(recipientCategory);

		Long scheduleEmailTaskId = this.addScheduleEmailTask(scheduleEmailTask);

		// 2. 逐筆建立紀錄
		for (T recipient : recipients) {
			String htmlContent = contentReplacer.apply(sendEmailDTO.getHtmlContent(), recipient);
			String plainText = contentReplacer.apply(sendEmailDTO.getPlainText(), recipient);

			ScheduleEmailRecord record = new ScheduleEmailRecord();
			record.setHtmlContent(htmlContent);
			record.setPlainText(plainText);
			record.setScheduleEmailTaskId(scheduleEmailTaskId);
			record.setRecipientCategory(recipientCategory);
			record.setStatus(ScheduleEmailStatus.PENDING.getValue());

			// 測試信件 vs 真實 Email
			if (sendEmailDTO.getIsTest()) {
				record.setEmail(sendEmailDTO.getTestEmail());
			} else {
				record.setEmail(emailExtractor.apply(recipient));
			}

			scheduleEmailRecordService.addScheduleEmailRecord(record);
		}
	}

	/**
	 * 根據日期，獲取Pending狀態的任務
	 * 
	 * @param date
	 * @return
	 */
	private List<ScheduleEmailTask> getPendingEmailTaskListByDate(LocalDateTime date) {
		// 1.截斷 任務時間 那天的開始時間
		LocalDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS); // 2025-08-07 00:00:00

		System.out.println("排程任務當天一早 " + startOfDay);

		// 2.加一天再減1秒得到當天結束
		LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1); // 2025-08-07 23:59:59

		System.out.println("排程任務當天結束 " + endOfDay);

		// 3.取出狀態為 pending 且執行日期與當前任務相同的資料
		LambdaQueryWrapper<ScheduleEmailTask> scheduleEmailTaskWrapper = new LambdaQueryWrapper<>();
		scheduleEmailTaskWrapper.eq(ScheduleEmailTask::getStatus, ScheduleEmailStatus.PENDING.getValue())
				.between(ScheduleEmailTask::getStartTime, startOfDay, endOfDay);
		return baseMapper.selectList(scheduleEmailTaskWrapper);
	}

	@Override
	public void deleteScheduleEmailTask(Long scheduleEmailTaskId) {
		baseMapper.deleteById(scheduleEmailTaskId);
	}

}
