package tw.com.topbs.service.impl;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.AsyncService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {

	private final JavaMailSender mailSender;

	// Semaphore 用來控制每次發送郵件之間的間隔
	private final Semaphore semaphore = new Semaphore(1);

	@Override
	@Async("taskExecutor")
	public void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent) {
		// 開始編寫信件,準備寄送單封郵件給會員
		try {
			MimeMessage message = mailSender.createMimeMessage();
			// message.setHeader("Content-Type", "text/html; charset=UTF-8");

			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(plainTextContent, false); // 纯文本版本
			helper.setText(htmlContent, true); // HTML 版本

			mailSender.send(message);

		} catch (MessagingException e) {
			System.err.println("發送郵件失敗: " + e.getMessage());
			log.error("發送郵件失敗: " + e.getMessage());
		}
	}

	@Override
	@Async("taskExecutor")
	public void sendCommonEmail(String to, String subject, String htmlContent, String plainTextContent,
			List<ByteArrayResource> attachments) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(plainTextContent, false); // 純文本版本
			helper.setText(htmlContent, true); // HTML 版本

			// 添加附件
			if (attachments != null && !(attachments.isEmpty())) {
				for (ByteArrayResource attachment : attachments) {
					helper.addAttachment(attachment.getFilename(), attachment);

				}
			}

			mailSender.send(message);

		} catch (MessagingException e) {
			System.err.println("發送郵件失敗: " + e.getMessage());
			log.error("發送郵件失敗: " + e.getMessage());
		}
	}

	@Override
	@Async("taskExecutor")
	public void sendGroupRegistrationEmail(Member member) {

		// 開始編寫信件,準備寄給一般註冊者找回密碼的信
		try {

			// 確保每次只能有一個線程執行發送郵件的邏輯
			semaphore.acquire();

			System.out.println("當前執行線程的會員為: " + member.getFirstName());

			MimeMessage message = mailSender.createMimeMessage();
			// message.setHeader("Content-Type", "text/html; charset=UTF-8");

			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(member.getEmail());
			helper.setSubject("2025 TOPBS & IOPBS Group Registration Successful");

			String categoryString;
			switch (member.getCategory()) {
			case 1 -> categoryString = "Non-member";
			case 2 -> categoryString = "Member";
			case 3 -> categoryString = "Others";
			default -> categoryString = "Unknown";
			}

			String htmlContent = """
					<!DOCTYPE html>
						<html >
							<head>
								<meta charset="UTF-8">
								<meta name="viewport" content="width=device-width, initial-scale=1.0">
								<title>Group Registration Successful</title>
								<style>
								    body { font-size: 1.2rem; line-height: 1.8; }
								    td { padding: 10px 0; }
								</style>
							</head>

							<body >
								<table>
									<tr>
					       				<td >
					           				<img src="https://topbs.zfcloud.cc/_nuxt/banner.DZ8Efg03.png" alt="Conference Banner"  width="640" style="max-width: 100%%; width: 640px; display: block;" object-fit:cover;">
					       				</td>
					   				</tr>
									<tr>
										<td style="font-size:2rem;">Welcome to 2025 TOPBS & IOBPS !</td>
									</tr>
									<tr>
										<td>We are pleased to inform you that your registration has been successfully completed.</td>
									</tr>
									<tr>
										<td>Your registration details are as follows:</td>
									</tr>
									<tr>
							            <td><strong>First Name:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Last Name:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Country:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Affiliation:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Job Title:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Phone:</strong> %s</td>
							        </tr>
							        <tr>
							            <td><strong>Category:</strong> %s</td>
							        </tr>
									<tr>
										<td>After logging in, please proceed with the payment of the registration fee.</td>
									</tr>
									<tr>
										<td>Completing this payment will grant you access to exclusive accommodation discounts and enable you to submit your work for the conference.</td>
									</tr>
									<tr>
										<td>If you have any questions, feel free to contact us. We look forward to seeing you at the conference!</td>
									</tr>
								</table>
							</body>
						</html>
					"""
					.formatted(member.getFirstName(), member.getLastName(), member.getCountry(),
							member.getAffiliation(), member.getJobTitle(), member.getPhone(), categoryString);

			String plainTextContent = "Welcome to 2025 TOPBS & IOBPS !\n"
					+ "Your Group registration has been successfully completed.\n"
					+ "Your registration details are as follows:\n" + "First Name: " + member.getFirstName() + "\n"
					+ "Last Name: " + member.getLastName() + "\n" + "Country: " + member.getCountry() + "\n"
					+ "Affiliation: " + member.getAffiliation() + "\n" + "Job Title: " + member.getJobTitle() + "\n"
					+ "Phone: " + member.getPhone() + "\n" + "Category: " + categoryString + "\n"
					+ "Please proceed with the payment of the registration fee to activate your accommodation discounts and submission features.\n"
					+ "If you have any questions, feel free to contact us. We look forward to seeing you at the conference!";
			helper.setText(plainTextContent, false); // 纯文本版本
			helper.setText(htmlContent, true); // HTML 版本

			mailSender.send(message);

			// 發送完一封後，進行延遲，隨機延遲 2-3 秒,2000 為兩秒
			long delay = 2000 + (long) (Math.random() * 1000);
			Thread.sleep(delay);

		} catch (MessagingException | InterruptedException e) {
			System.err.println("發送郵件失敗: " + e.getMessage());
			log.error("發送郵件失敗: " + e.getMessage());
		} finally {
			// 釋放信號量，允許其他線程繼續發送郵件
			semaphore.release();
		}

	}

}
