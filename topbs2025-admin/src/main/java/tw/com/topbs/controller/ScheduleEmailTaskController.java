package tw.com.topbs.controller;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;
import tw.com.topbs.service.ScheduleEmailTaskService;
import tw.com.topbs.system.pojo.DTO.LoginInfo;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 排程的電子郵件任務 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */

@Tag(name = "排程任務API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/schedule-email-task")
public class ScheduleEmailTaskController {

	private final ScheduleEmailTaskService scheduleEmailTaskService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一排程任務")
	@SaCheckRole("super-admin")
	public R<ScheduleEmailTask> getScheduleEmailTask(@PathVariable("id") Long taskId) {
		ScheduleEmailTask scheduleEmailTask = scheduleEmailTaskService.getScheduleEmailTask(taskId);
		return R.ok(scheduleEmailTask);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部會員(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<ScheduleEmailTask>> getScheduleEmailTaskPage(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) @Schema(description = "member會員、attendees與會者、paper投稿者、paperReviewer審稿委員") String recipientCategory,
			@RequestParam(required = false) @Schema(description = "0為pending、1為execute 、2為finished、3為failed、4為canceled") Integer status) {
		Page<ScheduleEmailTask> pageable = new Page<ScheduleEmailTask>(page, size);

		IPage<ScheduleEmailTask> scheduleEmailTaskPage = scheduleEmailTaskService
				.getScheduleEmailTaskPage(recipientCategory, status, pageable);

		return R.ok(scheduleEmailTaskPage);
	}

	@PutMapping("{id}")
	@Operation(summary = "取消單一排程任務")
	@SaCheckRole("super-admin")
	public R<ScheduleEmailTask> cancelScheduleEmailTask(@PathVariable("id") Long taskId) {
		scheduleEmailTaskService.cancelScheduleEmailTask(taskId);
		return R.ok();
	}

}
