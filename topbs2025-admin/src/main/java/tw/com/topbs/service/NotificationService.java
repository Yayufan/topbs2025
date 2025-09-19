package tw.com.topbs.service;

import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.entity.Member;

public interface NotificationService {

	/**
	 * 生成講者更新CV 或 照片時的通知信件內容
	 * 
	 * @param speakerName
	 * @param adminDashboardUrl
	 * @return
	 */
	EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl);

	/**
	 * 生成註冊成功的通知信件內容
	 * 
	 * @param member
	 * @param bannerPhotoUrl
	 * @return
	 */
	EmailBodyContent generateRegistrationSuccessContent(Member member, String bannerPhotoUrl);

}
