package tw.com.topbs.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.core.io.ByteArrayResource;

import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.pojo.entity.ScheduleEmailTask;

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
	 * @param to               收件者
	 * @param subject          主旨
	 * @param htmlContent      HTML內容
	 * @param plainTextContent 純文字內容
	 * @param attachments      附件檔案列表
	 */
	void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent,
			List<ByteArrayResource> attachments);

	/**
	 * 呼叫時觸發一個線程，單獨去執行寄信任務，但因為他是團體報名信件，所以有使用Semaphore 控制線程讓他們列隊，每三秒放一個線程去寄信
	 * 
	 * @param member
	 */
	void sendGroupRegistrationEmail(Member member);

	/**
	 * 裡面會根據寄出10封信件等3秒的模式，避免控制寄信速率
	 * 
	 * @param <T>
	 * @param recipients      任何收件者列表,member、attendees、paper、paperReviewer 等
	 * @param sendEmailDTO    信件資訊
	 * @param emailExtractor  獲取收件者mail的方式
	 * @param contentReplacer 信件內容替換方式
	 */
	<T> void batchSendEmail(List<T> recipients, SendEmailDTO sendEmailDTO, Function<T, String> emailExtractor,
			BiFunction<String, T, String> contentReplacer);

	/**
	 * 裡面會根據寄出10封信件等3秒的模式，避免控制寄信速率，並可以攜帶附件
	 * 
	 * @param <T>
	 * @param recipients         任何收件者列表,member、attendees、paper、paperReviewer 等
	 * @param sendEmailDTO       信件資訊
	 * @param emailExtractor     獲取收件者mail的方式
	 * @param contentReplacer    信件內容替換方式
	 * @param attachmentProvider 附件提供的查詢方式
	 */
	public <T> void batchSendEmail(List<T> recipients, SendEmailDTO sendEmailDTO, Function<T, String> emailExtractor,
			BiFunction<String, T, String> contentReplacer, Function<T, List<ByteArrayResource>> attachmentProvider // ✅ 新增附件查詢
	);

	/**
	 * 排程信件觸發寄信
	 * 
	 * @param scheduleEmailRecord
	 */
	public void triggerSendEmail(ScheduleEmailTask scheduleEmailTask,List<ScheduleEmailRecord> scheduleEmailRecordList);

}
