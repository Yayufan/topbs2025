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
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.ScheduleEmailTaskService;

@Component
@RequiredArgsConstructor
public class MemberMailStrategy implements MailStrategy {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private final MemberService memberService;
	private final MemberTagService memberTagService;
	private final AsyncService asyncService;
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

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有會員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的會員人數
		Long memberCount = 0L;

		//初始化要寄信的會員
		List<Member> memberList = new ArrayList<>();

		//初始化 memberIdSet ，用於去重memberId
		Set<Long> memberIdSet = new HashSet<>();

		if (hasNoTag) {
			memberCount = memberService.lambdaQuery().count();
		} else {

			// 透過tag先找到符合的member關聯
			List<MemberTag> memberTagList = memberTagService.getMemberTagByTagIds(tagIdList);

			// 從關聯中取出memberId ，使用Set去重複的會員，因為會員有可能有多個Tag
			memberIdSet = memberTagList.stream().map(memberTag -> memberTag.getMemberId()).collect(Collectors.toSet());

			// 如果memberIdSet 至少有一個，則開始搜尋Member
			if (!memberIdSet.isEmpty()) {
				memberCount = memberService.lambdaQuery().in(Member::getMemberId, memberIdSet).count();
			}

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (currentQuota - pendingExpectedEmailVolumeByToday < memberCount) {
			throw new EmailException("本日寄信額度無法寄送 " + memberCount + " 封信");
		} else if (memberCount <= 0) {
			throw new EmailException("沒有符合資格的會員");
		}

		// 前面都已經透過總數先排除了 額度不足、沒有符合資格會員的狀況，現在實際來獲取收信者名單
		// 沒有篩選任何Tag的，則給他所有Member名單
		memberList = this.getMemberListByTagIds(tagIdList);

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		//		asyncService.batchSendEmailToMembers(memberList, sendEmailDTO);

		asyncService.batchSendEmail(memberList, sendEmailDTO, Member::getEmail, this::replaceMemberMergeTag);

		// 額度直接扣除 查詢到的會員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-memberCount);

	}

	@Override
	public void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {
		// 1.查找要寄出的列表
		List<Member> memberList = this.getMemberListByTagIds(tagIdList);

		// 2.放入排程任務
		scheduleEmailTaskService.processScheduleEmailTask(sendEmailDTO, memberList, "member", Member::getEmail,
				this::replaceMemberMergeTag);
	}

	private String replaceMemberMergeTag(String content, Member member) {

		String newContent;
		MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(member.getCategory());

		newContent = content.replace("{{title}}", Strings.nullToEmpty(member.getTitle()))
				.replace("{{firstName}}", Strings.nullToEmpty(member.getFirstName()))
				.replace("{{lastName}}", Strings.nullToEmpty(member.getLastName()))
				.replace("{{email}}", Strings.nullToEmpty(member.getEmail()))
				.replace("{{phone}}", Strings.nullToEmpty(member.getPhone()))
				.replace("{{country}}", Strings.nullToEmpty(member.getCountry()))
				.replace("{{affiliation}}", Strings.nullToEmpty(member.getAffiliation()))
				.replace("{{jobTitle}}", Strings.nullToEmpty(member.getJobTitle()))
				.replace("{{category}}", memberCategoryEnum.getLabelEn());

		return newContent;

	}

	private List<Member> getMemberListByTagIds(Collection<Long> tagIdList) {
		// 1.先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有會員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		// 2.初始化要寄信的會員
		List<Member> memberList = new ArrayList<>();

		// 3.初始化 memberIdSet ，用於去重memberId
		Set<Long> memberIdSet = new HashSet<>();

		// 4.如果沒給tag代表要寄給全部人，如果有則透過tag找尋要寄送的名單
		if (hasNoTag) {
			memberList = memberService.lambdaQuery().list();
		} else {

			// 透過tag先找到符合的member關聯
			List<MemberTag> memberTagList = memberTagService.getMemberTagByTagIds(tagIdList);

			// 從關聯中取出memberId ，使用Set去重複的會員，因為會員有可能有多個Tag
			memberIdSet = memberTagList.stream().map(memberTag -> memberTag.getMemberId()).collect(Collectors.toSet());

			// 如果memberIdSet 至少有一個，則開始搜尋Member
			if (!memberIdSet.isEmpty()) {
				memberList = memberService.lambdaQuery().in(Member::getMemberId, memberIdSet).list();
			}

		}

		return memberList;

	}

}
