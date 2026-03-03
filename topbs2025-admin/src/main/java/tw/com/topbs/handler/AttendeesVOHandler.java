package tw.com.topbs.handler;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.AttendeesHistoryService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.MemberService;

@Component
@RequiredArgsConstructor
public class AttendeesVOHandler {

	private final MemberService memberService;
	private final AttendeesService attendeesService;
	private final AttendeesConvert attendeesConvert;
	private final AttendeesHistoryService attendeesHistoryService;

	/**
	 * 根據 attendeesId 獲取 與會者完整資訊
	 * 
	 * @param attendeesId
	 * @return
	 */
	public AttendeesVO getAttendeesVO(Long attendeesId) {
		// 1.查詢到與會者資訊
		Attendees attendees = attendeesService.getAttendees(attendeesId);
		// 2.查詢此與會者的基本資料
		Member member = memberService.getMember(attendees.getMemberId());
		// 3.attendees 轉換成 VO
		AttendeesVO attendeesVO = attendeesConvert.entityToVO(attendees);
		// 4.獲取是否為往年與會者		
		Boolean existsAttendeesHistory = attendeesHistoryService.existsAttendeesHistory(LocalDate.now().getYear() - 1,
				member.getIdCard(), member.getEmail());

		// 5.組裝VO
		attendeesVO.setMember(member);
		attendeesVO.setIsLastYearAttendee(existsAttendeesHistory);

		return attendeesVO;
	}

	/**
	 * 根據與會者列表,獲取AttendeesVO列表
	 * 
	 * @param attendeesList
	 * @return
	 */
	public List<AttendeesVO> getAttendeesVOsByAttendeesList(Collection<Attendees> attendeesList) {

		// 1.根據與會者列表對應的memberId 整合成List,並拿到memberList 
		Map<Long, Member> memberMap = memberService.getMemberMapByAttendeesList(attendeesList);

		// 2.最後組裝成AttendeesVO列表
		List<AttendeesVO> attendeesVOList = attendeesList.stream().map(attendees -> {
			AttendeesVO vo = attendeesConvert.entityToVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			return vo;
		}).collect(Collectors.toList());

		return attendeesVOList;

	}

	/**
	 * 根據與會者ID列表,獲取AttendeesVO列表
	 * 
	 * @param ids
	 * @return
	 */
	public List<AttendeesVO> getAttendeesVOsByAttendeesIds(Collection<Long> ids) {
		// 1.根據ids 查詢與會者列表
		List<Attendees> attendeesList = attendeesService.getAttendeesListByIds(ids);

		// 2.根據與會者列表對應的memberId 整合成List,並拿到memberList 
		Map<Long, Member> memberMap = memberService.getMemberMapByAttendeesList(attendeesList);

		// 最後組裝成AttendeesVO列表
		List<AttendeesVO> attendeesVOList = attendeesList.stream().map(attendees -> {
			AttendeesVO vo = attendeesConvert.entityToVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			return vo;
		}).collect(Collectors.toList());

		return attendeesVOList;
	};

}
