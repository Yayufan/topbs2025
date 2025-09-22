package tw.com.topbs.manager;

import org.springframework.stereotype.Component;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.MemberLoginInfo;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.NotificationService;

@RequiredArgsConstructor
@Component
public class MemberAuthManager {

	private final MemberService memberService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	/**
	 * 會員登入
	 * 
	 * @param memberLoginInfo
	 * @return
	 */
	public SaTokenInfo login(MemberLoginInfo memberLoginInfo) {
		return memberService.login(memberLoginInfo);
	}

	/**
	 * 會員登出
	 * 
	 */
	public void logout() {
		memberService.logout();
	};

	/**
	 * 忘記密碼
	 * 
	 * @param email
	 */
	public void forgetPassword(String email) {

		// 1.先透過email查找是否為註冊過的會員
		Member member = memberService.getMemberByEmail(email);

		// 2.產生找回密碼的信件內容
		EmailBodyContent retrieveContent = notificationService.generateRetrieveContent(member.getPassword());

		// 3.將密碼寄送到信箱
		asyncService.sendCommonEmail(email, "Retrieve password", retrieveContent.getHtmlContent(),
				retrieveContent.getPlainTextContent());

	}
}
