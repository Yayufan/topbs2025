package tw.com.topbs.service.impl;

import java.util.List;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.EmailTemplateConvert;
import tw.com.topbs.mapper.EmailTemplateMapper;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddEmailTemplateDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutEmailTemplateDTO;
import tw.com.topbs.pojo.entity.EmailTemplate;
import tw.com.topbs.service.EmailTemplateService;

/**
 * <p>
 * 信件模板表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-16
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl extends ServiceImpl<EmailTemplateMapper, EmailTemplate>
		implements EmailTemplateService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";

	private final EmailTemplateConvert emailTemplateConvert;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public List<EmailTemplate> getAllEmailTemplate() {
		List<EmailTemplate> emailTemplateList = baseMapper.selectList(null);
		return emailTemplateList;
	}

	@Override
	public IPage<EmailTemplate> getAllEmailTemplate(Page<EmailTemplate> page) {
		Page<EmailTemplate> emailTemplatePage = baseMapper.selectPage(page, null);
		return emailTemplatePage;
	}

	@Override
	public EmailTemplate getEmailTemplate(Long emailTemplateId) {
		EmailTemplate emailTemplate = baseMapper.selectById(emailTemplateId);
		return emailTemplate;
	}

	@Override
	public Long insertEmailTemplate(AddEmailTemplateDTO insertEmailTemplateDTO) {
		EmailTemplate emailTemplate = emailTemplateConvert.insertDTOToEntity(insertEmailTemplateDTO);
		baseMapper.insert(emailTemplate);
		return emailTemplate.getEmailTemplateId();
	}

	@Override
	public void updateEmailTemplate(PutEmailTemplateDTO updateEmailTemplateDTO) {
		EmailTemplate emailTemplate = emailTemplateConvert.updateDTOToEntity(updateEmailTemplateDTO);
		baseMapper.updateById(emailTemplate);
	}

	@Override
	public void deleteEmailTemplate(Long emailTemplateId) {
		// TODO Auto-generated method stub
		baseMapper.deleteById(emailTemplateId);

	}

	@Override
	public void deleteEmailTemplate(List<Long> emailTemplateIdList) {
		// TODO Auto-generated method stub
		for (Long emailTemplateId : emailTemplateIdList) {
			deleteEmailTemplate(emailTemplateId);
		}

	}

	@Async("taskExecutor") // 指定使用的線程池
	@Override
	public void sendEmail(SendEmailDTO sendEmailDTO) {

		/**
		 * 
		 * // 開始編寫信件給通過的會員
		 * LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		 * 
		 * // 寄信給狀態為審核通過,且具有MemberCode的會員
		 * memberQueryWrapper.eq(Member::getStatus, "1").gt(Member::getCode, 0);
		 * 
		 * List<Member> memberList = memberMapper.selectList(memberQueryWrapper);
		 * 
		 * for (Member member : memberList) {
		 * try {
		 * MimeMessage message = mailSender.createMimeMessage();
		 * // message.setHeader("Content-Type", "text/html; charset=UTF-8");
		 * 
		 * MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
		 * 
		 * helper.setTo(member.getEmail());
		 * helper.setSubject(sendEmailDTO.getSubject());
		 * 
		 * String htmlContent = sendEmailDTO.getHtmlContent();
		 * String plainTextContent = sendEmailDTO.getPlainText();
		 * 
		 * // 將 memberCode 格式化為 HA0001, HA0002, ..., HA9999
		 * String formattedMemberCode = String.format("HA%04d", member.getCode());
		 * 
		 * // 替換 {{memberName}} 和 {{memberCode}} 為真正的會員數據
		 * htmlContent = htmlContent.replace("{{memberName}}",
		 * member.getName()).replace("{{memberCode}}",
		 * formattedMemberCode);
		 * 
		 * plainTextContent = plainTextContent.replace("{{memberName}}",
		 * member.getName())
		 * .replace("{{memberCode}}", formattedMemberCode);
		 * 
		 * helper.setText(plainTextContent, false); // 纯文本版本
		 * helper.setText(htmlContent, true); // HTML 版本
		 * 
		 * mailSender.send(message);
		 * 
		 * } catch (MessagingException e) {
		 * 
		 * System.err.println("發送郵件失敗: " + e.getMessage());
		 * }
		 * }
		 * 
		 */
	}

	@Override
	public Long getDailyEmailQuota() {
		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		long currentQuota = quota.get();
		return currentQuota;
	}

}
