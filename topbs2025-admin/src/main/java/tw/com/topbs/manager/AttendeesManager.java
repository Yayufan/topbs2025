package tw.com.topbs.manager;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.mapper.AttendeesHistoryMapper;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesHistory;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;

@Component
@RequiredArgsConstructor
public class AttendeesManager {

	private final AttendeesMapper attendeesMapper;
	private final AttendeesConvert attendeesConvert;
	private final MemberMapper memberMapper;
	private final AttendeesHistoryMapper attendeesHistoryMapper;

	public Attendees getAttendeesByMemberId(Long memberId) {
		LambdaQueryWrapper<Attendees> attendeesWrapper = new LambdaQueryWrapper<>();
		attendeesWrapper.eq(Attendees::getMemberId, memberId);

		return attendeesMapper.selectOne(attendeesWrapper);
	}

	/**
	 * 獲得attendeesList對象
	 * 
	 * @return
	 */
	public List<Attendees> getAttendeesList() {
		List<Attendees> attendeesList = attendeesMapper.selectAttendees();
		return attendeesList;
	}

	/**
	 * 獲得attendeesPage對象
	 * 
	 * @param pageInfo
	 * @return
	 */
	public IPage<Attendees> getAttendeesPage(Page<Attendees> pageInfo) {
		return attendeesMapper.selectPage(pageInfo, null);
	}

	/**
	 * 根據 memberIds 獲得 attendeesPage對象
	 * 
	 * @param pageInfo
	 * @param memberIds
	 * @return
	 */
	public IPage<Attendees> getAttendeesPageByMemberIds(Page<Attendees> pageInfo, Collection<Long> memberIds) {
		LambdaQueryWrapper<Attendees> attendeesWrapper = new LambdaQueryWrapper<>();
		attendeesWrapper.in(Attendees::getMemberId, memberIds);
		Page<Attendees> attendeesPage = attendeesMapper.selectPage(pageInfo, attendeesWrapper);

		return attendeesPage;
	}

	/**
	 * 透過與會者ID得到AttendeesVO
	 * 
	 * @param attendeesId
	 * @return
	 */
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

	/**
	 * 根據與會者ID列表,獲取AttendeesVO列表
	 * 
	 * @param ids
	 * @return
	 */
	public List<AttendeesVO> getAttendeesVOByAttendeesIds(Collection<Long> ids) {
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

	/**
	 * 查詢是否為往年與會者
	 * 
	 * @param year
	 * @param idCard
	 * @param email
	 * @return
	 */
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

		// 回傳 true：資料庫有符合條件的紀錄 (result 不為 null)
		// 回傳 false：資料庫無符合條件的紀錄 (result 為 null)
		return result != null;
	}

	/**
	 * 透過與會者列表和一系列映射組裝成AttendeesTagVO
	 * 
	 * @param attendeesList    與會者 列表
	 * @param checkinMap       與會者ID-簽到記錄 映射
	 * @param checkinStatusMap 與會者ID-最後簽到狀態 映射
	 * @param attendeesTagMap  與會者ID-標籤ID關聯 映射
	 * @param tagMap           標籤ID-標籤 映射
	 * @param memberMap        會員ID-會員映射
	 * @return
	 */
	public List<AttendeesTagVO> buildAttendeesTagVOList(List<Attendees> attendeesList,
			Map<Long, List<CheckinRecord>> checkinMap, Map<Long, Boolean> checkinStatusMap,
			Map<Long, List<Long>> attendeesTagMap, Map<Long, Tag> tagMap, Map<Long, Member> memberMap) {
		return attendeesList.stream().map(attendees -> {
			AttendeesTagVO vo = attendeesConvert.entityToAttendeesTagVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			vo.setCheckinRecordList(checkinMap.getOrDefault(attendees.getAttendeesId(), Collections.emptyList()));
			vo.setIsCheckedIn(checkinStatusMap.getOrDefault(attendees.getAttendeesId(), false));

			List<Long> tagIds = attendeesTagMap.getOrDefault(attendees.getAttendeesId(), Collections.emptyList());
			Set<Tag> tagSet = tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toSet());
			vo.setTagSet(tagSet);

			return vo;
		}).collect(Collectors.toList());
	}

	/**
	 * 透過會員ID去刪除與會者，並返回與會者ID
	 * 
	 * @param memberId 會員ID
	 * @return attendeesId 被刪除的與會者ID
	 */
	public Long deleteAttendeesByMemberId(Long memberId) {

		Attendees attendees = this.getAttendeesByMemberId(memberId);

		//如果這個會員還不是與會者,則直接返回null
		if (attendees == null) {
			return null;
		}

		// 不為null,刪除與會者
		attendeesMapper.deleteById(attendees);

		return attendees.getAttendeesId();

	}

}
