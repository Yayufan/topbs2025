package tw.com.topbs.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.pojo.DTO.SendEmailByTagDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.com.topbs.pojo.entity.EmailTemplate;
import tw.com.topbs.service.EmailTemplateService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 信件模板表 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-01-16
 */

@Tag(name = "信件模板API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/email-template")
public class EmailTemplateController {

	private final EmailTemplateService emailTemplateService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一信件模板")
	public R<EmailTemplate> getEmailTemplate(@PathVariable("id") Long emailTemplateId) {
		EmailTemplate emailTemplate = emailTemplateService.getEmailTemplate(emailTemplateId);
		return R.ok(emailTemplate);
	}

	@GetMapping
	@Operation(summary = "查詢所有信件模板")
	public R<List<EmailTemplate>> getAllEmailTemplate() {

		List<EmailTemplate> emailTemplateList = emailTemplateService.getAllEmailTemplate();
		return R.ok(emailTemplateList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢所有信件模板(分頁)")
	public R<IPage<EmailTemplate>> getAllEmailTemplate(@RequestParam Integer page, @RequestParam Integer size) {
		Page<EmailTemplate> pageInfo = new Page<>(page, size);
		IPage<EmailTemplate> emailTemplateList = emailTemplateService.getAllEmailTemplate(pageInfo);
		return R.ok(emailTemplateList);
	}

	@Operation(summary = "新增信件模板")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@PostMapping
	public R<Long> saveEmailTemplate(@RequestBody AddEmailTemplateDTO insertEmailTemplateDTO) {
		Long emailTemplateId = emailTemplateService.insertEmailTemplate(insertEmailTemplateDTO);
		return R.ok(emailTemplateId);

	}

	@Operation(summary = "更新信件模板")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@PutMapping
	public R<Void> updateEmailTemplate(@RequestBody PutEmailTemplateDTO updateEmailTemplateDTO) {
		System.out.println("獲取到的DTO: " + updateEmailTemplateDTO);
		emailTemplateService.updateEmailTemplate(updateEmailTemplateDTO);
		return R.ok();

	}

	@Operation(summary = "刪除信件模板")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@DeleteMapping("{id}")
	public R<Void> deleteEmailTemplate(@PathVariable("id") Long emailTemplateId) {
		emailTemplateService.deleteEmailTemplate(emailTemplateId);
		return R.ok();

	}

	@Operation(summary = "批量刪除信件模板")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@DeleteMapping()
	public R<Void> deleteEmailTemplate(List<Long> emailTemplateIdList) {
		emailTemplateService.deleteEmailTemplate(emailTemplateIdList);
		return R.ok();

	}


	@GetMapping("email-quota")
	@Operation(summary = "查詢本日寄信餘額")
	public R<Long> getEmailQuota() {
		Long dailyEmailQuota = emailTemplateService.getDailyEmailQuota();
		return R.ok(dailyEmailQuota);
	}
	
	@Operation(summary = "寄送信件給會員，可根據tag來篩選寄送")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PostMapping("send-email")
	public R<Void> sendEmail(@Validated @RequestBody SendEmailByTagDTO sendEmailByTagDTO) {

		if (sendEmailByTagDTO.getSendEmailDTO().getIsSchedule()) {

			// 判斷是否有給執行日期
			if (sendEmailByTagDTO.getSendEmailDTO().getScheduleTime() == null) {
				throw new EmailException("未填寫排程日期");
			}
			
			// 判斷排程時間必須嚴格比當前時間 + 30分鐘更晚
			LocalDateTime scheduleTime = sendEmailByTagDTO.getSendEmailDTO().getScheduleTime();
			LocalDateTime minAllowedTime = LocalDateTime.now().plusMinutes(30);

			if (!scheduleTime.isAfter(minAllowedTime)) {
			    throw new EmailException("排程時間必須晚於當前時間至少30分鐘");
			}

			// 排程寄信為True 則走排程
			emailTemplateService.scheduleEmail(sendEmailByTagDTO.getTagIdList(), sendEmailByTagDTO.getSendEmailDTO());
		}else {
			// 排程寄信為False 則走立即寄信
			emailTemplateService.sendEmail(sendEmailByTagDTO.getTagIdList(), sendEmailByTagDTO.getSendEmailDTO());
		}
		
		return R.ok();

	}

}
