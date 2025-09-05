package tw.com.topbs.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;
import java.util.function.Function;

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
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.utils.MinioUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncServiceImpl implements AsyncService {

	private final JavaMailSender mailSender;
	private final PaperReviewerFileService paperReviewerFileService;

	private final MinioUtil minioUtil;

	private final String EMAIL_FROM = "notify@iopbs2025.org.tw";
	private final String EMAIL_FROM_NAME = "IOPBS 2025 Notification";
	private final String EMAIL_REPLY_TO = "iopbs2025@gmail.com";

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
			try {
				helper.setFrom(EMAIL_FROM, EMAIL_FROM_NAME);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// 指定回信信箱
			helper.setReplyTo(EMAIL_REPLY_TO);

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

			// 處理多個收件人地址
			String[] recipients = parseEmailAddresses(to);

			// 當使用SMTP中繼時,可以在SPF + DKIM + DMARC 驗證通過的domain 使用自己的domain
			// 可以跟brevo 的 smtp Server不一樣
			try {
				helper.setFrom(EMAIL_FROM, EMAIL_FROM_NAME);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 指定回信信箱
			helper.setReplyTo(EMAIL_REPLY_TO);

			helper.setTo(recipients);

			//			helper.setTo(to);

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

	/**
	 * 解析郵件地址字串，支援單個地址或逗號分隔的多個地址
	 * 
	 * @param emailString 郵件地址字串
	 * @return 郵件地址陣列
	 */
	private String[] parseEmailAddresses(String emailString) {
		if (emailString == null || emailString.trim().isEmpty()) {
			throw new IllegalArgumentException("郵件地址不能為空");
		}

		// 移除首尾空白並按逗號分割
		String[] addresses = emailString.trim().split(",");

		// 清理每個地址的空白字符並驗證
		for (int i = 0; i < addresses.length; i++) {
			addresses[i] = addresses[i].trim();
		}

		return addresses;
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

			// 當使用SMTP中繼時,可以在SPF + DKIM + DMARC 驗證通過的domain 使用自己的domain
			// 可以跟brevo 的 smtp Server不一樣
			try {
				helper.setFrom(EMAIL_FROM, EMAIL_FROM_NAME);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 指定回信信箱
			helper.setReplyTo(EMAIL_REPLY_TO);

			helper.setTo(member.getEmail());
			helper.setSubject("2025 TOPBS & IOPBS Group Registration Successful");

			MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(member.getCategory());

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
										<td>For any inquiries, please contact iopbs2025@gmail.com</td>
									</tr>
								</table>
							</body>
						</html>
					"""
					.formatted(member.getFirstName(), member.getLastName(), member.getCountry(),
							member.getAffiliation(), member.getJobTitle(), member.getPhone(),
							memberCategoryEnum.getLabelEn(), member.getEmail(), member.getPassword());

			String plainTextContent = "Welcome to 2025 TOPBS & IOBPS !\n"
					+ "Your Group registration has been successfully completed.\n"
					+ "Your registration details are as follows:\n" + "First Name: " + member.getFirstName() + "\n"
					+ "Last Name: " + member.getLastName() + "\n" + "Country: " + member.getCountry() + "\n"
					+ "Affiliation: " + member.getAffiliation() + "\n" + "Job Title: " + member.getJobTitle() + "\n"
					+ "Phone: " + member.getPhone() + "\n" + "Category: " + memberCategoryEnum.getLabelEn() + "\n"
					+ "Account: " + member.getEmail() + "\n" + "Password: " + member.getPassword() + "\n"
					+ "Please proceed with the payment of the registration fee to activate your accommodation discounts and submission features.\n"
					+ "For any inquiries, please contact iopbs2025@gmail.com";
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
	public <T> void batchSendEmail(List<T> recipients, SendEmailDTO sendEmailDTO, Function<T, String> emailExtractor,
			BiFunction<String, T, String> contentReplacer

	) {
		int batchSize = 10; // 每批寄信數量
		long delayMs = 3000L; // 每批間隔

		// 使用 Guava partition 分批
		List<List<T>> batches = Lists.partition(recipients, batchSize);

		for (List<T> batch : batches) {
			for (T recipient : batch) {
				// 1. 個人化內容
				String htmlContent = contentReplacer.apply(sendEmailDTO.getHtmlContent(), recipient);
				String plainText = contentReplacer.apply(sendEmailDTO.getPlainText(), recipient);

				// 2. 測試信件 vs 真實收件者
				String email = sendEmailDTO.getIsTest() ? sendEmailDTO.getTestEmail() : emailExtractor.apply(recipient);

				// 3. 寄信
				this.sendCommonEmail(email, sendEmailDTO.getSubject(), htmlContent, plainText);
			}

			try {
				Thread.sleep(delayMs); // ✅ 控速，避免被信箱伺服器擋
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
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

				List<ByteArrayResource> attachments = new ArrayList<>();

				// 判斷是否需要攜帶官方文件
				if (sendEmailDTO.getIncludeOfficialAttachment()) {

					// 獲取審稿委員的附件檔案
					List<PaperReviewerFile> paperReviewerFiles = paperReviewerFileService
							.getPaperReviewerFilesByPaperReviewerId(paperReviewer.getPaperReviewerId());

					for (PaperReviewerFile paperReviewerFile : paperReviewerFiles) {

						try {
							// 獲取檔案位元組
							byte[] fileBytes = minioUtil.getFileBytes(paperReviewerFile.getPath());

							if (fileBytes != null) {

								ByteArrayResource resource = new ByteArrayResource(fileBytes) {
									@Override
									public String getFilename() {
										return paperReviewerFile.getFileName();
									}
								};
								attachments.add(resource);
							}

						} catch (Exception e) {
							System.out.println("無法讀取檔案:, 錯誤: " + paperReviewerFile.getPath() + e.getMessage());
						}
					}

				}

				String htmlContent = this.replacePaperReviewerMergeTag(sendEmailDTO.getHtmlContent(), paperReviewer);
				String plainText = this.replacePaperReviewerMergeTag(sendEmailDTO.getPlainText(), paperReviewer);

				// 當今天為測試信件，則將信件全部寄送給測試信箱
				if (sendEmailDTO.getIsTest()) {
					this.sendCommonEmail(sendEmailDTO.getTestEmail(), sendEmailDTO.getSubject(), htmlContent, plainText,
							attachments);
				} else {
					// 內部觸發sendCommonEmail時不會額外開闢一個線程，因為@Async是讓整個ServiceImpl 代表一個線程
					this.sendCommonEmail(paperReviewer.getEmail(), sendEmailDTO.getSubject(), htmlContent, plainText,
							attachments);

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

}
