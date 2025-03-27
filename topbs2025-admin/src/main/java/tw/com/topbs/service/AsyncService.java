package tw.com.topbs.service;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;

import tw.com.topbs.pojo.entity.Member;

public interface AsyncService {

	/**
	 * 寄送單獨用戶的信件使用，呼叫時觸發一個線程，單獨去執行寄信任務，加速呼叫API的響應速度
	 * 
	 * @param to               收件者
	 * @param subject          主旨
	 * @param htmlContent      HTML內容
	 * @param plainTextContent 純文字內容
	 */
	void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent);

	
	/**
	 * 寄送單獨用戶的信件使用，呼叫時觸發一個線程，單獨去執行寄信任務，加速呼叫API的響應速度(可攜帶附件)
	 * 
	 * @param to 收件者
	 * @param subject 主旨
	 * @param htmlContent HTML內容
	 * @param plainTextContent 純文字內容
	 * @param attachments 附件檔案列表
	 */
	void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent, List<ByteArrayResource> attachments);
	
	/**
	 * 呼叫時觸發一個線程，單獨去執行寄信任務，但因為他是團體報名信件，所以有使用Semaphore 控制線程讓他們列隊，每三秒放一個線程去寄信
	 * 
	 * @param member
	 */
	void sendGroupRegistrationEmail(Member member);

}
