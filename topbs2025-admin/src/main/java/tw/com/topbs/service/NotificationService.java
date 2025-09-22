package tw.com.topbs.service;

import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.entity.Member;

public interface NotificationService {

	/**
	 * 生成註冊成功的通知信件內容
	 * 
	 * @param member
	 * @param bannerPhotoUrl
	 * @return
	 */
	EmailBodyContent generateRegistrationSuccessContent(Member member, String bannerPhotoUrl);
	
	/**
	 * 生成 團體報名 註冊成功的通知信件內容
	 * 
	 * @param member
	 * @param bannerPhotoUrl
	 * @return
	 */
	EmailBodyContent generateGroupRegistrationSuccessContent(Member member, String bannerPhotoUrl);
	

	/**
	 * 生成 找回密碼 的通知信件內容
	 * 
	 * @param password
	 * @return
	 */
	EmailBodyContent generateRetrieveContent(String password);
	
	/**
	 * 生成講者更新CV 或 照片時的通知信件內容
	 * 
	 * @param speakerName
	 * @param adminDashboardUrl
	 * @return
	 */
	EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl);

}
