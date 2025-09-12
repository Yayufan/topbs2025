package tw.com.topbs.strategy.mail;

import java.util.List;

import tw.com.topbs.pojo.DTO.SendEmailDTO;

public interface MailStrategy {

	/**
	 * 立刻寄信
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void sendEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 排程寄信
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

}
