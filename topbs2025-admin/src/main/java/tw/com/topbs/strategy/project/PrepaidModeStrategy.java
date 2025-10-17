package tw.com.topbs.strategy.project;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.config.RegistrationFeeConfig;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.enums.RegistrationPhaseEnum;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.NotificationService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.utils.CountryUtil;

@Component
@RequiredArgsConstructor
public class PrepaidModeStrategy implements ProjectModeStrategy {

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	@Value("${project.group-size}")
	private int GROUP_SIZE;

	private RegistrationFeeConfig registrationFeeConfig;

	private final OrdersService ordersService;
	private final SettingService settingService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	/**
	 * 預付款模式<br>
	 * 計算會員的註冊費，建立訂單，並寄信通知
	 * 
	 */
	@Override
	public void handleRegistration(Member member) {

		// 1.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();

		// 2.透過Country 拿到國籍 , 只分國內國外,	
		String country = CountryUtil.getTaiwanOrForeign(member.getCountry());

		// 3.拿到身分
		MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(member.getCategory());

		// 4.透過階段、國籍、身分，得到金額
		BigDecimal membershipFee = registrationFeeConfig.getFee(registrationPhaseEnum.getValue(), country,
				memberCategoryEnum.getConfigKey());

		// 5.創建註冊費訂單
		ordersService.createRegistrationOrder(membershipFee, member);

		// 6.創建註冊成功通知信件內容
		EmailBodyContent registrationSuccessContent = notificationService.generateRegistrationSuccessContent(member,
				BANNER_PHOTO_URL);

		// 7.異步寄送信件
		asyncService.sendCommonEmail(member.getEmail(), PROJECT_NAME + " Registration Successful",
				registrationSuccessContent.getHtmlContent(), registrationSuccessContent.getPlainTextContent());

	}

	@Override
	public void handleGroupRegistration(Member member, boolean isMaster, BigDecimal totalFee) {
		if (isMaster) {
			// Master 負責付錢
			ordersService.createGroupRegistrationOrder(totalFee, member);
		} else {
			// Slave 不付錢，0元訂單，未付款
			ordersService.createFreeGroupRegistrationOrder(member);
		}
		
		
		// 2.產生系統團體報名通知信
		EmailBodyContent groupRegistrationSuccessContent = notificationService
				.generateGroupRegistrationSuccessContent(member, BANNER_PHOTO_URL);

		// 3.寄信個別通知會員，團體報名成功
		asyncService.sendCommonEmail(member.getEmail(), PROJECT_NAME + " GROUP Registration Successful",
				groupRegistrationSuccessContent.getHtmlContent(),
				groupRegistrationSuccessContent.getPlainTextContent());
		
	}

	@Override
	public void handlePaperSubmission(String memberId, Object paperRequest) {
		// TODO Auto-generated method stub

	}

}
