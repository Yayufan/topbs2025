package tw.com.topbs.scheduler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.enums.ScheduleEmailStatus;
import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.ScheduleEmailTaskService;

@RequiredArgsConstructor
@Component
@Slf4j
public class AsyncTriggerScheduleEmailTask {

	private final AsyncService asyncService;
	private final ScheduleEmailTaskService scheduleEmailTaskService;

	@Qualifier("taskExecutor")
	private final Executor taskExecutor;

	// 使用 Cron 表達式設置定時任務 (每分鐘第零秒執行此任務，測試時使用)
	//	@Scheduled(cron = "0 * * * * ?")
	// 使用 Cron 表達式設置定時任務 (每天凌晨2點執行 cron = "0 0 2 * * ?" )
	@Scheduled(cron = "0 * * * * ?")
	public void triggerScheduleEmailTask() {
		// 1.獲取應該執行的任務
		List<ScheduleEmailTask> processDueTasks = scheduleEmailTaskService.getProcessDueTasks();

		// 2.判斷有無任務需要執行
		if (processDueTasks.isEmpty()) {
			return;
		}

		// 3.把所有任務的 status 設成 EXECUTE，避免下次此排程工作再被觸發
		processDueTasks.forEach(task -> task.setStatus(ScheduleEmailStatus.EXECUTE.getValue()));
		scheduleEmailTaskService.saveOrUpdateBatch(processDueTasks);

		// 4.遍歷需要執行的任務
		for (ScheduleEmailTask processDueTask : processDueTasks) {

			log.info("執行排程任務: " + processDueTask.getScheduleEmailTaskId());

			// 獲取該次寄信任務的所有信件內容
			List<ScheduleEmailRecord> taskRecords = scheduleEmailTaskService
					.getTaskRecordsBytaskId(processDueTask.getScheduleEmailTaskId());

			// 使用異步任務,讓寄信任務不要因為上個任務被堵塞,
			CompletableFuture.runAsync(() -> {
				asyncService.triggerSendEmail(processDueTask, taskRecords);
				processDueTask.setStatus(ScheduleEmailStatus.FINISHED.getValue());

			}, taskExecutor).handle((result, ex) -> {
				// result 必定為null別管它
				// 當出現異常,修改任務狀態為FAILED
				if (ex != null) {
					// 處理異常情況
					log.error("整體任務出現異常: ", ex.getMessage());
					processDueTask.setStatus(ScheduleEmailStatus.FAILED.getValue());
				}
				// 無論成功失敗都會執行更新任務狀態
				scheduleEmailTaskService.updateById(processDueTask);
				return null;

			});
		}
		
		 

	}
}
