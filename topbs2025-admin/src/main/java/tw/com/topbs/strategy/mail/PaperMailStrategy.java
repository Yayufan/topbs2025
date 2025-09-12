package tw.com.topbs.strategy.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.ScheduleEmailTaskService;

@Component
@RequiredArgsConstructor
public class PaperMailStrategy implements MailStrategy {

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private final AsyncService asyncService;
	private final PaperService paperService;
	private final PaperTagService paperTagService;
	private final ScheduleEmailTaskService scheduleEmailTaskService;

	@Override
	public void batchSendEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		// 獲取本日預計要寄出的信件量, 為了保證排程任務順利被寄出
		int pendingExpectedEmailVolumeByToday = scheduleEmailTaskService.getPendingExpectedEmailVolumeByToday();

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有稿件(通訊作者)
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的稿件(通訊作者)人數
		Long paperCount = 0L;

		//初始化要寄信的稿件(通訊作者)
		List<Paper> paperList = new ArrayList<>();

		//初始化 paperIdSet ，用於去重paperId
		Set<Long> paperIdSet = new HashSet<>();

		if (hasNoTag) {
			paperCount = paperService.lambdaQuery().count();
		} else {
			// 透過tag先找到符合的paper關聯
			List<PaperTag> paperTagList = paperTagService.getPaperTagBytagIdList(tagIdList);

			// 從關聯中取出paperId ，使用Set去重複的稿件(通訊作者)，因為稿件(通訊作者)有可能有多個Tag
			paperIdSet = paperTagList.stream().map(paperTag -> paperTag.getPaperId()).collect(Collectors.toSet());

			// 如果paperIdSet 至少有一個，則開始搜尋Member
			if (!paperIdSet.isEmpty()) {
				paperCount = paperService.lambdaQuery().in(Paper::getPaperId, paperIdSet).count();
			}

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (currentQuota - pendingExpectedEmailVolumeByToday < paperCount) {
			throw new EmailException("本日寄信額度無法寄送 " + paperCount + " 封信");
		} else if (paperCount <= 0) {
			throw new EmailException("沒有符合資格的稿件(通訊作者)");
		}

		// 前面都已經透過總數先排除了 額度不足、沒有符合資格稿件(通訊作者)的狀況，現在實際來獲取收信者名單
		// 沒有篩選任何Tag的，則給他所有Member名單
		paperList = this.getPaperListByTagIds(tagIdList);

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信，這邊是寄給
		asyncService.batchSendEmail(paperList, sendEmailDTO, Paper::getCorrespondingAuthorEmail,
				this::replacePaperMergeTag);

		// 額度直接扣除 查詢到的稿件(通訊作者)數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-paperCount);

	}

	@Override
	public void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {
		// 1.獲取投稿者列表
		List<Paper> paperList = this.getPaperListByTagIds(tagIdList);

		// 2.放入排程任務
		scheduleEmailTaskService.processScheduleEmailTask(sendEmailDTO, paperList, "paper",
				Paper::getCorrespondingAuthorEmail, this::replacePaperMergeTag);
	}

	private List<Paper> getPaperListByTagIds(Collection<Long> tagIdList) {
		// 1.先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有投稿者(通訊作者)
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		// 2.初始化要寄信的投稿者(通訊作者)
		List<Paper> paperList = new ArrayList<>();

		// 3.初始化 paperIdSet ，用於去重paperId
		Set<Long> paperIdSet = new HashSet<>();

		// 4.如果沒給tag代表要寄給全部人，如果有則透過tag找尋要寄送的名單
		if (hasNoTag) {
			paperList = paperService.lambdaQuery().list();
		} else {

			// 透過tag先找到符合的member關聯
			List<PaperTag> paperTagList = paperTagService.getPaperTagBytagIdList(tagIdList);

			// 從關聯中取出memberId ，使用Set去重複的會員，因為會員有可能有多個Tag
			paperIdSet = paperTagList.stream().map(paperTag -> paperTag.getPaperId()).collect(Collectors.toSet());

			// 如果memberIdSet 至少有一個，則開始搜尋Member
			if (!paperIdSet.isEmpty()) {
				paperList = paperService.lambdaQuery().in(Paper::getPaperId, paperIdSet).list();
			}

		}

		return paperList;

	}

	private String replacePaperMergeTag(String content, Paper paper) {
		String newContent;

		newContent = content.replace("{{absType}}", Strings.nullToEmpty(paper.getAbsType()))
				.replace("{{absProp}}", Strings.nullToEmpty(paper.getAbsProp()))
				.replace("{{absTitle}}", Strings.nullToEmpty(paper.getAbsTitle()))
				.replace("{{firstAuthor}}", Strings.nullToEmpty(paper.getFirstAuthor()))
				.replace("{{speaker}}", Strings.nullToEmpty(paper.getSpeaker()))
				.replace("{{speakerAffiliation}}", Strings.nullToEmpty(paper.getSpeakerAffiliation()))
				.replace("{{correspondingAuthor}}", Strings.nullToEmpty(paper.getCorrespondingAuthor()))
				.replace("{{correspondingAuthorEmail}}", Strings.nullToEmpty(paper.getCorrespondingAuthorEmail()));

		return newContent;
	}

}
