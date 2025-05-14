package tw.com.topbs.manager;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.mapper.AttendeesHistoryMapper;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesHistory;
import tw.com.topbs.pojo.entity.Member;

@Component
@RequiredArgsConstructor
public class AttendeesManager {

	private final AttendeesMapper attendeesMapper;
	private final AttendeesConvert attendeesConvert;
	private final MemberMapper memberMapper;
	private final AttendeesHistoryMapper attendeesHistoryMapper;

	public List<Attendees> getAttendeesList() {
		List<Attendees> attendeesList = attendeesMapper.selectAttendees();
		return attendeesList;
	}

	public AttendeesVO getAttendeesVOByAttendeesId(Long attendeesId) {
		// 1.先查詢到與會者自己的紀錄
		Attendees attendees = attendeesMapper.selectById(attendeesId);

		// 2.從attendees的 attendeesId中找到與會者的基本資料
		Member member = memberMapper.selectById(attendees.getMemberId());

		// 3.attendees 轉換成 VO
		AttendeesVO attendeesVO = attendeesConvert.entityToVO(attendees);

		// 4.獲取是否為往年與會者
		Boolean existsAttendeesHistory = this.existsAttendeesHistory(LocalDate.now().getYear() - 1, member.getIdCard(),
				member.getEmail());

		// 5.組裝VO
		attendeesVO.setMember(member);
		attendeesVO.setIsLastYearAttendee(existsAttendeesHistory);

		return attendeesVO;
	};

	public List<AttendeesVO> getAttendeesVOByIds(Collection<Long> ids) {
		// 根據ids 查詢與會者列表
		List<Attendees> attendeesList = attendeesMapper.selectBatchIds(ids);

		// 根據與會者列表對應的memberId 整合成List,並拿到memberList 
		List<Long> memberIds = attendeesList.stream().map(Attendees::getMemberId).collect(Collectors.toList());
		List<Member> memberList = memberMapper.selectBatchIds(memberIds);

		// 透過Member製成映射關係
		Map<Long, Member> memberMap = memberList.stream()
				.collect(Collectors.toMap(Member::getMemberId, Function.identity()));

		// 最後組裝成AttendeesVO列表
		List<AttendeesVO> attendeesVOList = attendeesList.stream().map(attendees -> {
			AttendeesVO vo = attendeesConvert.entityToVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			return vo;
		}).collect(Collectors.toList());

		return attendeesVOList;
	};

	private Boolean existsAttendeesHistory(Integer year, String idCard, String email) {
		LambdaQueryWrapper<AttendeesHistory> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AttendeesHistory::getYear, year);

		if (idCard != null && !idCard.isBlank()) {
			wrapper.eq(AttendeesHistory::getIdCard, idCard);
		} else {
			wrapper.eq(AttendeesHistory::getEmail, email);
		}

		// 有可能為null 有可能查詢有值
		AttendeesHistory result = attendeesHistoryMapper.selectOne(wrapper);

		System.out.println("result 的值為" + result);

		// 回傳 true：資料庫有符合條件的紀錄 (result 不為 null)
		// 回傳 false：資料庫無符合條件的紀錄 (result 為 null)
		return result != null;
	}

}
