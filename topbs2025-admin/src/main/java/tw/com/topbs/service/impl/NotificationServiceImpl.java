package tw.com.topbs.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.service.NotificationService;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final SpringTemplateEngine templateEngine;

	@Override
	public EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl) {
		Context context = new Context();
		context.setVariable("conferenceName", "IOPBS 2025");
		context.setVariable("speakerName", speakerName);
		context.setVariable("updateTime",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable("updatedItems", "CV and Profile Photo");
		context.setVariable("adminDashboardUrl", adminDashboardUrl);
		context.setVariable("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		context.setVariable("currentYear", String.valueOf(LocalDate.now().getYear()));

		String htmlContent = templateEngine.process("html/speaker-update-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/speaker-update-notification.txt", context);

		// 返回具有HTML 和 純文字的兩種信件內容 EmailBodyContent 
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

}
