package tw.com.topbs.service.impl;

import java.util.List;
import java.util.concurrent.Semaphore;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.exception.RegistrationInfoException;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperReviewer;
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

			// 當使用SMTP中繼時,可以在SPF + DKIM + DMARC 驗證通過的domain 使用自己的domain
			// 可以跟brevo 的 smtp Server不一樣
//			helper.setFrom("amts-joey@zhongfu-pr.com.tw","TICBCS 大會系統");
			
			helper.setTo(to);
			helper.setSubject(subject);
			//			helper.setText(plainTextContent, false); // 纯文本版本
			//			helper.setText(htmlContent, true); // HTML 版本

			helper.setText(plainTextContent, htmlContent);

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
			//			helper.setText(plainTextContent, false); // 純文本版本
			//			helper.setText(htmlContent, true); // HTML 版本
			helper.setText(plainTextContent, htmlContent);

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
					           				<img src="https://iopbs2025.org.tw/_nuxt/banner.CL2lyu9P.png" alt="Conference Banner"  width="640" style="max-width: 100%%; width: 640px; display: block;" object-fit:cover;">
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
							            <td><strong>Account:</strong> %s</td>
							        </tr>
							        							        <tr>
							            <td><strong>Password:</strong> %s</td>
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
							member.getAffiliation(), member.getJobTitle(), member.getPhone(), categoryString,
							member.getEmail(), member.getPassword());

			String plainTextContent = "Welcome to 2025 TOPBS & IOBPS !\n"
					+ "Your Group registration has been successfully completed.\n"
					+ "Your registration details are as follows:\n" + "First Name: " + member.getFirstName() + "\n"
					+ "Last Name: " + member.getLastName() + "\n" + "Country: " + member.getCountry() + "\n"
					+ "Affiliation: " + member.getAffiliation() + "\n" + "Job Title: " + member.getJobTitle() + "\n"
					+ "Phone: " + member.getPhone() + "\n" + "Category: " + categoryString + "\n" + "Account: "
					+ member.getEmail() + "\n" + "Password: " + member.getPassword() + "\n"
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

	@Override
	@Async("taskExecutor")
	public void batchSendEmailToMembers(List<Member> memberList, SendEmailDTO sendEmailDTO) {

		// 批量寄信數量
		int batchSize = 10;
		// 批量寄信間隔 3000 毫秒
		long delayMs = 3000L;

		/**
		 * 把一個 List<T> 拆成若干個小清單（subList），每組大小為 batchSize：
		 * List<String> names = Arrays.asList("A", "B", "C", "D", "E");
		 * List<List<String>> batches = Lists.partition(names, 2);
		 * 
		 * // 結果： [["A", "B"], ["C", "D"], ["E"]]
		 * 
		 * 
		 */
		List<List<Member>> batches = Lists.partition(memberList, batchSize);

		for (List<Member> batch : batches) {
			for (Member member : batch) {
				String htmlContent = this.replaceMemberMergeTag(sendEmailDTO.getHtmlContent(), member);
				String plainText = this.replaceMemberMergeTag(sendEmailDTO.getPlainText(), member);

				// 當今天為測試信件，則將信件全部寄送給測試信箱
				if (sendEmailDTO.getIsTest()) {
					this.sendCommonEmail(sendEmailDTO.getTestEmail(), sendEmailDTO.getSubject(), htmlContent,
							plainText);
				} else {
					// 內部觸發sendCommonEmail時不會額外開闢一個線程，因為@Async是讓整個ServiceImpl 代表一個線程
					this.sendCommonEmail(member.getEmail(), sendEmailDTO.getSubject(), htmlContent, plainText);
				}

			}

			try {
				Thread.sleep(delayMs); // ✅ 控速，避免信箱被擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	private String replaceMemberMergeTag(String content, Member member) {

		String newContent;

		String categoryStr;

		// 當前時間處於(早鳥優惠 - 註冊截止時間)之間，金額變動
		categoryStr = switch (member.getCategory()) {
		// Non-member 的註冊費價格
		case 1 -> "Non-member";
		// Member 的註冊費價格
		case 2 -> "Member";
		// Others 的註冊費價格
		case 3 -> "Others";
		default -> throw new RegistrationInfoException("category is not in system");
		};

		newContent = content.replace("{{title}}", member.getTitle())
				.replace("{{firstName}}", member.getFirstName())
				.replace("{{lastName}}", member.getLastName())
				.replace("{{email}}", member.getEmail())
				.replace("{{phone}}", member.getPhone())
				.replace("{{country}}", member.getCountry())
				.replace("{{affiliation}}", member.getAffiliation())
				.replace("{{jobTitle}}", member.getJobTitle())
				.replace("{{category}}", categoryStr);

		return newContent;

	}

	@Override
	@Async("taskExecutor")
	public void batchSendEmailToCorrespondingAuthor(List<Paper> paperList, SendEmailDTO sendEmailDTO) {

		// 批量寄信數量
		int batchSize = 10;
		// 批量寄信間隔 3000 毫秒
		long delayMs = 3000L;

		/**
		 * 把一個 List<T> 拆成若干個小清單（subList），每組大小為 batchSize：
		 * List<String> names = Arrays.asList("A", "B", "C", "D", "E");
		 * List<List<String>> batches = Lists.partition(names, 2);
		 * 
		 * // 結果： [["A", "B"], ["C", "D"], ["E"]]
		 * 
		 * 
		 */
		List<List<Paper>> batches = Lists.partition(paperList, batchSize);

		for (List<Paper> batch : batches) {
			for (Paper paper : batch) {
				String htmlContent = this.replacePaperMergeTag(sendEmailDTO.getHtmlContent(), paper);
				String plainText = this.replacePaperMergeTag(sendEmailDTO.getPlainText(), paper);

				// 當今天為測試信件，則將信件全部寄送給測試信箱
				if (sendEmailDTO.getIsTest()) {
					this.sendCommonEmail(sendEmailDTO.getTestEmail(), sendEmailDTO.getSubject(), htmlContent,
							plainText);
				} else {
					// 內部觸發sendCommonEmail時不會額外開闢一個線程，因為@Async是讓整個ServiceImpl 代表一個線程
					this.sendCommonEmail(paper.getCorrespondingAuthorEmail(), sendEmailDTO.getSubject(), htmlContent,
							plainText);

				}

			}

			try {
				Thread.sleep(delayMs); // ✅ 控速，避免信箱被擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	private String replacePaperMergeTag(String content, Paper paper) {
		String newContent;

		newContent = content.replace("{{absType}}", paper.getAbsType())
				.replace("{{absProp}}", paper.getAbsProp())
				.replace("{{absTitle}}", paper.getAbsTitle())
				.replace("{{firstAuthor}}", paper.getFirstAuthor())
				.replace("{{speaker}}", paper.getSpeaker())
				.replace("{{speakerAffiliation}}", paper.getSpeakerAffiliation())
				.replace("{{correspondingAuthor}}", paper.getCorrespondingAuthor())
				.replace("{{correspondingAuthorEmail}}", paper.getCorrespondingAuthorEmail());

		return newContent;

	}

	@Override
	@Async("taskExecutor")
	public void batchSendEmailToPaperReviewer(List<PaperReviewer> paperReviewerList, SendEmailDTO sendEmailDTO) {

		// 批量寄信數量
		int batchSize = 10;
		// 批量寄信間隔 3000 毫秒
		long delayMs = 3000L;

		/**
		 * 把一個 List<T> 拆成若干個小清單（subList），每組大小為 batchSize：
		 * List<String> names = Arrays.asList("A", "B", "C", "D", "E");
		 * List<List<String>> batches = Lists.partition(names, 2);
		 * 
		 * // 結果： [["A", "B"], ["C", "D"], ["E"]]
		 * 
		 * 
		 */
		List<List<PaperReviewer>> batches = Lists.partition(paperReviewerList, batchSize);

		for (List<PaperReviewer> batch : batches) {
			for (PaperReviewer paperReviewer : batch) {
				String htmlContent = this.replacePaperReviewerMergeTag(sendEmailDTO.getHtmlContent(), paperReviewer);
				String plainText = this.replacePaperReviewerMergeTag(sendEmailDTO.getPlainText(), paperReviewer);

				// 當今天為測試信件，則將信件全部寄送給測試信箱
				if (sendEmailDTO.getIsTest()) {
					this.sendCommonEmail(sendEmailDTO.getTestEmail(), sendEmailDTO.getSubject(), htmlContent,
							plainText);
				} else {
					// 內部觸發sendCommonEmail時不會額外開闢一個線程，因為@Async是讓整個ServiceImpl 代表一個線程
					this.sendCommonEmail(paperReviewer.getEmail(), sendEmailDTO.getSubject(), htmlContent, plainText);

				}

			}

			try {
				Thread.sleep(delayMs); // ✅ 控速，避免信箱被擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private String replacePaperReviewerMergeTag(String content, PaperReviewer paperReviewer) {
		String newContent;

		newContent = content.replace("{{{absTypeList}}", paperReviewer.getAbsTypeList())
				.replace("{{email}}", paperReviewer.getEmail())
				.replace("{{name}}", paperReviewer.getName())
				.replace("{{phone}}", paperReviewer.getPhone())
				.replace("{{account}}", paperReviewer.getAccount())
				.replace("{{password}}", paperReviewer.getPassword());

		return newContent;
	}

	@Override
	@Async("taskExecutor")
	public void batchSendEmailToAttendeess(List<AttendeesVO> attendeesVOList, SendEmailDTO sendEmailDTO) {
		// 批量寄信數量
		int batchSize = 10;
		// 批量寄信間隔 3000 毫秒
		long delayMs = 3000L;

		/**
		 * 把一個 List<T> 拆成若干個小清單（subList），每組大小為 batchSize：
		 * List<String> names = Arrays.asList("A", "B", "C", "D", "E");
		 * List<List<String>> batches = Lists.partition(names, 2);
		 * 
		 * // 結果： [["A", "B"], ["C", "D"], ["E"]]
		 * 
		 * 
		 */
		List<List<AttendeesVO>> batches = Lists.partition(attendeesVOList, batchSize);

		for (List<AttendeesVO> batch : batches) {
			for (AttendeesVO attendeesVO : batch) {
				String htmlContent = this.replaceAttendeesMergeTag(sendEmailDTO.getHtmlContent(), attendeesVO);
				String plainText = this.replaceAttendeesMergeTag(sendEmailDTO.getPlainText(), attendeesVO);

				// 當今天為測試信件，則將信件全部寄送給測試信箱
				if (sendEmailDTO.getIsTest()) {
					this.sendCommonEmail(sendEmailDTO.getTestEmail(), sendEmailDTO.getSubject(), htmlContent,
							plainText);
				} else {
					// 內部觸發sendCommonEmail時不會額外開闢一個線程，因為@Async是讓整個ServiceImpl 代表一個線程
					this.sendCommonEmail(attendeesVO.getMember().getEmail(), sendEmailDTO.getSubject(), htmlContent,
							plainText);

				}

			}

			try {
				Thread.sleep(delayMs); // ✅ 控速，避免信箱被擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

	}

	private String replaceAttendeesMergeTag(String content, AttendeesVO attendeesVO) {

		String qrCodeUrl = String.format("https://iopbs.org.tw/prod-api/attendees/qrcode?attendeesId=%s",
				attendeesVO.getAttendeesId());

		String newContent = content.replace("{{QRcode}}", "<img src=\"" + qrCodeUrl + "\" alt=\"QR Code\" />")
				.replace("{{name}}", attendeesVO.getMember().getChineseName());

		return newContent;

	}

}
