package tw.com.topbs.strategy.mail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperReviewerTagService;
import tw.com.topbs.service.ScheduleEmailTaskService;
import tw.com.topbs.utils.MinioUtil;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaperReviewerMailStrategy implements MailStrategy {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private final PaperReviewerService paperReviewerService;
	private final PaperReviewerTagService paperReviewerTagService;
	private final PaperReviewerFileService paperReviewerFileService;
	private final AsyncService asyncService;
	private final ScheduleEmailTaskService scheduleEmailTaskService;
	private final MinioUtil minioUtil;

	@Override
	public void sendEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		// 獲取本日預計要寄出的信件量, 為了保證排程任務順利被寄出
		int pendingExpectedEmailVolumeByToday = scheduleEmailTaskService.getPendingExpectedEmailVolumeByToday();

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有審稿委員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的審稿委員人數
		Long paperReviewerCount = 0L;

		//初始化要寄信的審稿委員
		List<PaperReviewer> paperReviewerList = new ArrayList<>();

		//初始化 paperReviewerIdSet ，用於去重paperReviewerId
		Set<Long> paperReviewerIdSet = new HashSet<>();

		// 拿到人數,避免直接查詢造成OOM
		if (hasNoTag) {
			paperReviewerCount = paperReviewerService.lambdaQuery().count();
		} else {
			// 透過tag先找到符合的paperReviewer關聯
			List<PaperReviewerTag> paperReviewerTagList = paperReviewerTagService.lambdaQuery()
					.in(PaperReviewerTag::getTagId, tagIdList)
					.list();

			// 從關聯中取出paperReviewerId ，使用Set去重複的審稿委員，因為審稿委員有可能有多個Tag
			paperReviewerIdSet = paperReviewerTagList.stream()
					.map(paperReviewerTag -> paperReviewerTag.getPaperReviewerId())
					.collect(Collectors.toSet());

			// 如果paperReviewerIdSet 至少有一個，則開始搜尋PaperReviewer
			if (!paperReviewerIdSet.isEmpty()) {
				paperReviewerCount = paperReviewerService.lambdaQuery()
						.in(PaperReviewer::getPaperReviewerId, paperReviewerIdSet)
						.count();
			}

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (currentQuota - pendingExpectedEmailVolumeByToday < paperReviewerCount) {
			throw new EmailException("本日寄信額度無法寄送 " + paperReviewerCount + " 封信");
		} else if (paperReviewerCount <= 0) {
			throw new EmailException("沒有符合資格的審稿委員");
		}

		// 前面都已經透過總數先排除了 額度不足、沒有符合資格審稿委員的狀況，現在實際來獲取收信者名單
		// 沒有篩選任何Tag的，則給他所有PaperReviewer名單		
		paperReviewerList = this.getPaperReviewerList(tagIdList);

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		asyncService.batchSendEmail(paperReviewerList, sendEmailDTO, PaperReviewer::getEmail,
				this::replacePaperReviewerMergeTag, this::getReviewerAttachments);

		// 額度直接扣除 查詢到的審稿委員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-paperReviewerCount);

	}

	@Override
	public void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {
		// 1.拿到審稿委員名單
		List<PaperReviewer> paperReviewerList = this.getPaperReviewerList(tagIdList);

		// 2.納入排程寄信
		scheduleEmailTaskService.processScheduleEmailTask(sendEmailDTO, paperReviewerList, "paperReviewer",
				PaperReviewer::getEmail, this::replacePaperReviewerMergeTag, this::getReviewerAttachmentsPath);

	}

	private List<PaperReviewer> getPaperReviewerList(List<Long> tagIdList) {
		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有審稿委員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化 paperReviewerIdSet ，用於去重paperReviewerId
		Set<Long> paperReviewerIdSet = new HashSet<>();

		//初始化 paperReviewerList 
		List<PaperReviewer> paperReviewerList = new ArrayList<>();

		if (hasNoTag) {
			paperReviewerList = paperReviewerService.lambdaQuery().list();
		} else {
			// 透過tag先找到符合的paperReviewer關聯
			List<PaperReviewerTag> paperReviewerTagList = paperReviewerTagService.lambdaQuery()
					.in(PaperReviewerTag::getTagId, tagIdList)
					.list();

			// 從關聯中取出paperReviewerId ，使用Set去重複的審稿委員，因為審稿委員有可能有多個Tag
			paperReviewerIdSet = paperReviewerTagList.stream()
					.map(paperReviewerTag -> paperReviewerTag.getPaperReviewerId())
					.collect(Collectors.toSet());

			// 如果paperReviewerIdSet 至少有一個，則開始搜尋PaperReviewer
			if (!paperReviewerIdSet.isEmpty()) {
				paperReviewerList = paperReviewerService.lambdaQuery()
						.in(PaperReviewer::getPaperReviewerId, paperReviewerIdSet)
						.list();
			}

		}

		return paperReviewerList;

	}

	private String replacePaperReviewerMergeTag(String content, PaperReviewer paperReviewer) {
		String newContent;

		newContent = content.replace("{{{absTypeList}}", Strings.nullToEmpty(paperReviewer.getAbsTypeList()))
				.replace("{{email}}", Strings.nullToEmpty(paperReviewer.getEmail()))
				.replace("{{name}}", Strings.nullToEmpty(paperReviewer.getName()))
				.replace("{{phone}}", Strings.nullToEmpty(paperReviewer.getPhone()))
				.replace("{{account}}", Strings.nullToEmpty(paperReviewer.getAccount()))
				.replace("{{password}}", Strings.nullToEmpty(paperReviewer.getPassword()));

		return newContent;
	}

	private List<ByteArrayResource> getReviewerAttachments(PaperReviewer reviewer) {

		// 要返回的附件
		List<ByteArrayResource> attachments = new ArrayList<>();

		// 獲取審稿委員的附件檔案
		List<PaperReviewerFile> paperReviewerFiles = paperReviewerFileService
				.getPaperReviewerFilesByPaperReviewerId(reviewer.getPaperReviewerId());

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
				log.error("無法讀取檔案:, 錯誤: " + paperReviewerFile.getPath() + e.getMessage());
			}
		}
		return attachments;
	}

	private List<String> getReviewerAttachmentsPath(PaperReviewer reviewer) {
		// 要返回的附件
		List<String> attachmentsPath = new ArrayList<>();

		// 獲取審稿委員的附件檔案
		List<PaperReviewerFile> paperReviewerFiles = paperReviewerFileService
				.getPaperReviewerFilesByPaperReviewerId(reviewer.getPaperReviewerId());

		for (PaperReviewerFile paperReviewerFile : paperReviewerFiles) {
			// 拿到檔案的minio路徑,並添加進List中
			attachmentsPath.add(paperReviewerFile.getPath());
		}
		return attachmentsPath;
	}

}
