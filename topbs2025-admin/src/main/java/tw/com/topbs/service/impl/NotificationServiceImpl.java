package tw.com.topbs.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.NotificationService;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final SpringTemplateEngine templateEngine;

	@Override
	public EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl) {

		// 1.設置變量
		Context context = new Context();
		context.setVariable("conferenceName", "IOPBS 2025");
		context.setVariable("speakerName", speakerName);
		context.setVariable("updateTime",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable("updatedItems", "CV and Profile Photo");
		context.setVariable("adminDashboardUrl", adminDashboardUrl);
		context.setVariable("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		context.setVariable("currentYear", String.valueOf(LocalDate.now().getYear()));

		// 2.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/speaker-update-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/speaker-update-notification.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateRegistrationSuccessContent(Member member, String bannerPhotoUrl) {
		Context context = new Context();
		// 1.設置Banner 圖片
		context.setVariable("bannerPhotoUrl", bannerPhotoUrl);

		// 2.設置其他變量
		context.setVariable("conferenceName", "IOPBS 2025");
		context.setVariable("firstName", member.getFirstName());
		context.setVariable("lastName", member.getLastName());
		context.setVariable("country", member.getCountry());
		context.setVariable("affiliation", member.getAffiliation());
		context.setVariable("jobTitle", member.getJobTitle());
		context.setVariable("phone", member.getPhone());
		context.setVariable("updateTime",
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable("currentYear", String.valueOf(LocalDate.now().getYear()));

		// 3.Category 要轉換成字串
		context.setVariable("category", MemberCategoryEnum.fromValue(member.getCategory()).getLabelEn());

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/registration-success-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/registration-success-notification.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);

	}

}
