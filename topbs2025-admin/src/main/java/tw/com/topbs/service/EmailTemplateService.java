package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.com.topbs.pojo.entity.EmailTemplate;

/**
 * <p>
 * 信件模板表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-01-16
 */
public interface EmailTemplateService extends IService<EmailTemplate> {
	/**
	 * 獲取全部信件模板
	 * 
	 * @return
	 */
	List<EmailTemplate> getAllEmailTemplate();

	/**
	 * 獲取全部信件模板(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<EmailTemplate> getAllEmailTemplate(Page<EmailTemplate> page);

	/**
	 * 獲取單一信件模板
	 * 
	 * @param emailTemplateId
	 * @return
	 */
	EmailTemplate getEmailTemplate(Long emailTemplateId);

	/**
	 * 新增信件模板
	 * 
	 * @param insertEmailTemplateDTO
	 */
	Long insertEmailTemplate(AddEmailTemplateDTO insertEmailTemplateDTO);

	/**
	 * 更新信件模板
	 * 
	 * @param updateEmailTemplateDTO
	 */
	void updateEmailTemplate(PutEmailTemplateDTO updateEmailTemplateDTO);

	/**
	 * 根據EmailTemplateId刪除信件模板
	 * 
	 * @param emailTemplateId
	 */
	void deleteEmailTemplate(Long emailTemplateId);

	/**
	 * 批量刪除信件模板
	 * 
	 * @param emailTemplateIdList
	 */
	void deleteEmailTemplate(List<Long> emailTemplateIdList);

	/** -------------------------------------------------------- */

	/**
	 * 獲取本日寄信餘額
	 * 
	 * @return
	 */
	Long getDailyEmailQuota();

	/**
	 * 排程寄信
	 * 
	 * @param tagIdList    標籤ID列表
	 * @param sendEmailDTO 信件資訊
	 */
	void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 立刻寄信
	 * 
	 * @param tagIdList    標籤ID列表
	 * @param sendEmailDTO 信件資訊
	 */
	void sendEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

}
