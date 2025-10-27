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

	@Value("${project.language}")
	private String LANGUAGE;

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

		// 3. 根據 project.language 選擇模板路徑（無需 if-else 太多，簡單拼接）
		String languagePath = "";
		if ("zh_TW".equalsIgnoreCase(LANGUAGE)) {
			languagePath = "/zh_tw";
		}
		String htmlTemplatePath = "html" + languagePath + "/registration-success-notification.html";
		String textTemplatePath = "plain-text" + languagePath + "/registration-success-notification.txt";

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process(htmlTemplatePath, context);
		String plainTextContent = templateEngine.process(textTemplatePath, context);
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

		// 3. 根據 project.language 選擇模板路徑（無需 if-else 太多，簡單拼接）
		String languagePath = "";
		if ("zh_TW".equalsIgnoreCase(LANGUAGE)) {
			languagePath = "/zh_tw";
		}
		String htmlTemplatePath = "html" + languagePath + "/registration-success-notification.html";
		String textTemplatePath = "plain-text" + languagePath + "/registration-success-notification.txt";

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process(htmlTemplatePath, context);
		String plainTextContent = templateEngine.process(textTemplatePath, context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateRetrieveContent(String password) {
		// 1.設置通用變量
		Context context = new Context();
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_REPLY_TO, REPLY_TO);
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));

		// 2.設置 忘記密碼 變量
		context.setVariable("password", password);

		// 3. 根據 project.language 選擇模板路徑（無需 if-else 太多，簡單拼接）
		String languagePath = "";
		if ("zh_TW".equalsIgnoreCase(LANGUAGE)) {
			languagePath = "/zh_tw";
		}
		String htmlTemplatePath = "html" + languagePath + "/registration-success-notification.html";
		String textTemplatePath = "plain-text" + languagePath + "/registration-success-notification.txt";

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process(htmlTemplatePath, context);
		String plainTextContent = templateEngine.process(textTemplatePath, context);

		return new EmailBodyContent(htmlContent, plainTextContent);

	}

	@Override
	public EmailBodyContent generateAbstractSuccessContent(Paper paper) {

		// 1.設置通用變量
		Context context = new Context();
		context.setVariable(FIELD_BANNER_PHOTO_URL, BANNER_PHOTO_URL);
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_REPLY_TO, REPLY_TO);
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));

		// 2.設置 稿件 變量
		context.setVariable("paper", paper);

		// 3. 根據 project.language 選擇模板路徑（無需 if-else 太多，簡單拼接）
		String languagePath = "";
		if ("zh_TW".equalsIgnoreCase(LANGUAGE)) {
			languagePath = "/zh_tw";
		}
		String htmlTemplatePath = "html" + languagePath + "/registration-success-notification.html";
		String textTemplatePath = "plain-text" + languagePath + "/registration-success-notification.txt";

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process(htmlTemplatePath, context);
		String plainTextContent = templateEngine.process(textTemplatePath, context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl) {

		// 1.設置通用變量
		Context context = new Context();
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);
		context.setVariable(FIELD_UPDATE_TIME,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		context.setVariable(FIELD_CURRENT_YEAR, String.valueOf(LocalDate.now().getYear()));

		// 2.設置講者更新資料變量
		context.setVariable("speakerName", speakerName);
		context.setVariable("updatedItems", "CV and Profile Photo");
		context.setVariable("adminDashboardUrl", adminDashboardUrl);

		// 3. 根據 project.language 選擇模板路徑（無需 if-else 太多，簡單拼接）
		String languagePath = "";
		if ("zh_TW".equalsIgnoreCase(LANGUAGE)) {
			languagePath = "/zh_tw";
		}
		String htmlTemplatePath = "html" + languagePath + "/registration-success-notification.html";
		String textTemplatePath = "plain-text" + languagePath + "/registration-success-notification.txt";

		// 4.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process(htmlTemplatePath, context);
		String plainTextContent = templateEngine.process(textTemplatePath, context);

		return new EmailBodyContent(htmlContent, plainTextContent);
	}

	@Override
	public EmailBodyContent generateWalkInRegistrationContent(Long attendeesId, String bannerPhotoUrl) {
		Context context = new Context();
		// 1.設置通用變量
		context.setVariable(FIELD_BANNER_PHOTO_URL, bannerPhotoUrl);
		context.setVariable(FIELD_CONFERENCE_NAME, PROJECT_NAME);

		// 2. 根據 project.language 選擇模板路徑（無需 if-else 太多，簡單拼接）
		String languagePath = "";
		if ("zh_TW".equalsIgnoreCase(LANGUAGE)) {
			languagePath = "/zh_tw";
		}
		String htmlTemplatePath = "html" + languagePath + "/registration-success-notification.html";
		String textTemplatePath = "plain-text" + languagePath + "/registration-success-notification.txt";

		// 3.產生具有HTML 和 純文字的兩種信件內容 EmailBodyContent  並返回
		String htmlContent = templateEngine.process(htmlTemplatePath, context);
		String plainTextContent = templateEngine.process(textTemplatePath, context);
		return new EmailBodyContent(htmlContent, plainTextContent);
	}

}
