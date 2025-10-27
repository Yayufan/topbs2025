package tw.com.topbs.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.enums.ProjectModeEnum;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.service.NotificationService;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

	private final SpringTemplateEngine templateEngine;

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.email.reply-to}")
	private String REPLY_TO;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	@Value("${project.mode}")
	private ProjectModeEnum mode;

	/**
	 * 固定通用的信件變量
	 */
	private static final String FIELD_BANNER_PHOTO_URL = "bannerPhotoUrl";
	private static final String FIELD_CONFERENCE_NAME = "conferenceName";
	private static final String FIELD_UPDATE_TIME = "updateTime";
	private static final String FIELD_CURRENT_YEAR = "currentYear";
	private static final String FIELD_REPLY_TO = "replyTo";
	private static final String FIELD_MODE = "mode";

	/**
	 * 註冊通知使用的信件變量
	 */
	private static final String FIELD_FIRST_NAME = "firstName";
	private static final String FIELD_LAST_NAME = "lastName";
	private static final String FIELD_COUNTRY = "country";
	private static final String FIELD_AFFILIATION = "affiliation";
	private static final String FIELD_JOB_TITLE = "jobTitle";
	private static final String FIELD_PHONE = "phone";
	private static final String FIELD_CATEGORY = "category";

	@Override
	public EmailBodyContent generateRegistrationSuccessContent(Member member, String bannerPhotoUrl) {
		Context context = new Context();

		// 1.設置通用變量
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_BANNER_PHOTO_URL, bannerPhotoUrl);
		context.setVariable(FIELD_MODE, mode.getValue());
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));
		context.setVariable(FIELD_REPLY_TO, REPLY_TO);

		// 2.設置註冊信變量
		context.setVariable(FIELD_FIRST_NAME, member.getFirstName());
		context.setVariable(FIELD_LAST_NAME, member.getLastName());
		context.setVariable(FIELD_COUNTRY, member.getCountry());
		context.setVariable(FIELD_AFFILIATION, member.getAffiliation());
		context.setVariable(FIELD_JOB_TITLE, member.getJobTitle());
		context.setVariable(FIELD_PHONE, member.getPhone());
		// Category 要轉換成字串
		context.setVariable(FIELD_CATEGORY, MemberCategoryEnum.fromValue(member.getCategory()).getLabelEn());

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/registration-success-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/registration-success-notification.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);

	}

	@Override
	public EmailBodyContent generateGroupRegistrationSuccessContent(Member member, String bannerPhotoUrl) {
		Context context = new Context();
		// 1.設置通用變量
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_BANNER_PHOTO_URL, bannerPhotoUrl);
		context.setVariable(FIELD_MODE, mode.getValue());
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));
		context.setVariable(FIELD_REPLY_TO, REPLY_TO);

		// 2.設置註冊信變量
		context.setVariable(FIELD_FIRST_NAME, member.getFirstName());
		context.setVariable(FIELD_LAST_NAME, member.getLastName());
		context.setVariable(FIELD_COUNTRY, member.getCountry());
		context.setVariable(FIELD_AFFILIATION, member.getAffiliation());
		context.setVariable(FIELD_JOB_TITLE, member.getJobTitle());
		context.setVariable(FIELD_PHONE, member.getPhone());
		// Category 要轉換成字串
		context.setVariable(FIELD_CATEGORY, MemberCategoryEnum.fromValue(member.getCategory()).getLabelEn());

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/group-registration-success-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/group-registration-success-notification.txt",
				context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateRetrieveContent(String password) {
		// 1.設置變量
		Context context = new Context();
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_REPLY_TO, REPLY_TO);
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));
		context.setVariable("password", password);

		// 2.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/retrieve-password.html", context);
		String plainTextContent = templateEngine.process("plain-text/retrieve-password.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);

	}

	@Override
	public EmailBodyContent generateAbstractSuccessContent(Paper paper) {
		// 1.設置變量
		Context context = new Context();

		// 1.設置Banner 圖片
		context.setVariable(FIELD_BANNER_PHOTO_URL, BANNER_PHOTO_URL);
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_REPLY_TO, REPLY_TO);
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));
		context.setVariable("paper", paper);

		// 2.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/abstract-success-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/abstract-success-notification.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl) {

		// 1.設置變量
		Context context = new Context();
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));
		context.setVariable("speakerName", speakerName);
		context.setVariable("updatedItems", "CV and Profile Photo");
		context.setVariable("adminDashboardUrl", adminDashboardUrl);

		// 2.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/speaker-update-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/speaker-update-notification.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateWalkInRegistrationContent(Long attendeesId, String bannerPhotoUrl) {
		Context context = new Context();
		// 1.設置Banner 圖片
		context.setVariable(FIELD_BANNER_PHOTO_URL, bannerPhotoUrl);

		// 2.設置其他變量
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process("html/walk-in-registration-notification.html", context);
		String plainTextContent = templateEngine.process("plain-text/walk-in-registration-notification.txt", context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

}
