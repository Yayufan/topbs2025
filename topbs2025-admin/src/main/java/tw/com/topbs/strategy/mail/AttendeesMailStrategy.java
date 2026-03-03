package tw.com.topbs.strategy.mail;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.ScheduleEmailTaskService;

@Component
@RequiredArgsConstructor
public class AttendeesMailStrategy implements MailStrategy {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private final AttendeesConvert attendeesConvert;
	private final AttendeesService attendeesService;
	private final AttendeesTagService attendeesTagService;
	private final AsyncService asyncService;
	private final ScheduleEmailTaskService scheduleEmailTaskService;
	private final MemberService memberService;

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

		//初始化 attendeesIdSet ，用於去重attendeesId
		Set<Long> attendeesIdSet = new HashSet<>();

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有會員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的會員人數
		Long attendeesCount = 0L;

		if (hasNoTag) {
			attendeesCount = attendeesService.lambdaQuery().count();
		} else {

			// 拿到與會者ID列表
			attendeesIdSet = this.getAttendeesIdSet(tagIdList);

			if (attendeesIdSet.isEmpty()) {
				throw new EmailException("沒有符合資格的與會者");
			}

			// 如果attendeesIdSet 至少有一個，則開始搜尋Attendees
			attendeesCount = attendeesService.lambdaQuery().in(Attendees::getAttendeesId, attendeesIdSet).count();

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (attendeesCount <= 0) {
			throw new EmailException("沒有符合資格的與會者");
		} else if (currentQuota - pendingExpectedEmailVolumeByToday < attendeesCount) {
			throw new EmailException("本日寄信額度無法寄送 " + attendeesCount + " 封信");
		}

		// 查收信者名單 + member
		List<AttendeesVO> attendeesVOList = buildAttendeesVOList(hasNoTag ? null : attendeesIdSet);

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		asyncService.batchSendEmail(attendeesVOList, sendEmailDTO, a -> a.getMember().getEmail(),
				this::replaceAttendeesMergeTag);

		// 額度直接扣除 查詢到的會員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-attendeesCount);
	}

	@Override
	public void scheduleEmail(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {
		// 1.拿到與會者ID 列表
		Set<Long> attendeesIdSet = this.getAttendeesIdSet(tagIdList);

		// 2.透過AttendeesID列表拿到 vo列表
		List<AttendeesVO> attendeesVOList = this.buildAttendeesVOList(attendeesIdSet);

		// 3.放入排程任務
		scheduleEmailTaskService.processScheduleEmailTask(sendEmailDTO, attendeesVOList, "attendees",
				a -> a.getMember().getEmail(), this::replaceAttendeesMergeTag);

	}

	/**
	 * 根據 tagIdList 獲取與會者 ID 集合
	 *
	 * @param tagIdList 標籤 ID 列表
	 * @return 與會者 ID 集合，若無標籤或無符合者，則返回空集合
	 */
	private Set<Long> getAttendeesIdSet(List<Long> tagIdList) {
		// 1.若 tagIdList 為空，則表示所有與會者，直接返回 null
		if (tagIdList == null || tagIdList.isEmpty()) {
			return null;
		}

		// 2.透過 tag 找到符合的 attendees 關聯
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByTagIds(tagIdList);

		// 3.從關聯中取出 attendeesId，並使用 Set 去重
		return attendeesTagList.stream().map(AttendeesTag::getAttendeesId).collect(Collectors.toSet());
	}

	/**
	 * 返回 與會者的VO 對象
	 * 
	 * @param attendeesIdSet 與會者的ID集合
	 * @return
	 */
	private List<AttendeesVO> buildAttendeesVOList(Set<Long> attendeesIdSet) {
		List<Attendees> attendeesList;
		if (attendeesIdSet == null || attendeesIdSet.isEmpty()) {
			attendeesList = attendeesService.lambdaQuery().list();
		} else {
			attendeesList = attendeesService.lambdaQuery().in(Attendees::getAttendeesId, attendeesIdSet).list();
		}

		Map<Long, Member> memberMap = memberService.getMemberMapByAttendeesList(attendeesList);

		// 組裝 VO
		return attendeesList.stream().map(attendees -> {
			AttendeesVO vo = attendeesConvert.entityToVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			return vo;
		}).collect(Collectors.toList());
	}

	private String replaceAttendeesMergeTag(String content, AttendeesVO attendeesVO) {

		String qrCodeUrl = String.format("https://iopbs.org.tw/prod-api/attendees/qrcode?attendeesId=%s",
				attendeesVO.getAttendeesId());

		String newContent = content.replace("{{QRcode}}", "<img src=\"" + qrCodeUrl + "\" alt=\"QR Code\" />")
				.replace("{{name}}", Strings.nullToEmpty(attendeesVO.getMember().getChineseName()));

		return newContent;

	}

}
