package tw.com.topbs.manager;

import java.util.Collection;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.CheckinRecordService;
import tw.com.topbs.service.MemberService;

@Component
@RequiredArgsConstructor
public class MemberManager {

	private final MemberService memberService;
	private final AttendeesService attendeesService;
	private final CheckinRecordService checkinRecordService;

	/**
	 * 刪除單個會員<br>
	 * 包含其與會者身分 和 簽到退紀錄
	 * 
	 * @param memberId
	 */
	@Transactional
	public void deleteMember(Long memberId) {
		// 1.刪除會員的與會者身分
		Attendees attendees = attendeesService.deleteAttendeesByMemberId(memberId);

		// 2.如果attendees不為null，刪除他的簽到/退紀錄
		if(attendees != null) {
			checkinRecordService.deleteCheckinRecordByAttendeesId(attendees.getAttendeesId());
		}


		// 3.最後刪除自身
		memberService.deleteMember(memberId);

	}

	/**
	 * 批量刪除單個會員<br>
	 * 包含其與會者身分 和 簽到退紀錄
	 * 
	 * @param memberId
	 */
	@Transactional
	public void deleteMemberList(Collection<Long> memberIds) {
		for (Long memberId : memberIds) {
			this.deleteMember(memberId);
		}
	}

}
