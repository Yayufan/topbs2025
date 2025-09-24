package tw.com.topbs.manager;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.enums.CheckinActionTypeEnum;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.CheckinRecordService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.TagService;

@Component
@RequiredArgsConstructor
public class AttendeesTagManager {

	private final MemberService memberService;
	private final AttendeesService attendeesService;
	private final AttendeesTagService attendeesTagService;
	private final AttendeesConvert attendeesConvert;
	private final CheckinRecordService checkinRecordService;

	/**
	 * 根據 attendeesId ， 獲取與會者資訊 和 Tag標籤
	 * 
	 * @param attendeesId
	 * @return
	 */
	public AttendeesTagVO getAttendeesTagVO(Long attendeesId) {
		// 1.獲取attendees 資料並轉換成 attendeesTagVO
		Attendees attendees = attendeesService.getAttendees(attendeesId);
		AttendeesTagVO attendeesTagVO = attendeesConvert.entityToAttendeesTagVO(attendees);

		// 2.查詢attendees 的基本資料，並放入Member屬性
		Member member = memberService.getMember(attendees.getMemberId());
		attendeesTagVO.setMember(member);

		// 3.根據 attendeesId 找到與會者所有簽到/退紀錄，並放入CheckinRecord屬性
		List<CheckinRecord> checkinRecordList = checkinRecordService.getCheckinRecordByAttendeesId(attendeesId);
		attendeesTagVO.setCheckinRecordList(checkinRecordList);

		// 4.isCheckedIn屬性預設是false, 所以只要判斷最新的資料是不是已簽到,如果是再進行更改就好
		CheckinRecord latest = checkinRecordList.stream()
				// ID 為雪花算法，等於時間序
				.max(Comparator.comparing(CheckinRecord::getCheckinRecordId))
				.orElse(null);

		if (latest != null && CheckinActionTypeEnum.CHECKIN.getValue().equals(latest.getActionType())) {
			attendeesTagVO.setIsCheckedIn(true);
		}

		// 5.查找 與會者 的tags，放入VO
		List<Tag> tags = attendeesTagService.getTagsByAttendeesId(attendeesId);
		attendeesTagVO.setTagList(tags);

		return attendeesTagVO;
	}

	/**
	 * 組裝AttendeesTagVO對象
	 * 
	 * @param attendeesPage
	 * @return
	 */
	private List<AttendeesTagVO> buildAttendeesTagVO(IPage<Attendees> attendeesPage) {
		// 2.獲取 會員 映射對象
		Map<Long, Member> memberMap = memberService.getMemberMap(attendeesPage.getRecords());

		// 3.獲取 簽到記錄 映射對象
		Map<Long, List<CheckinRecord>> checkinMap = checkinRecordService
				.getCheckinMapByAttendeesList(attendeesPage.getRecords());

		// 4.獲取 最後簽到紀錄 映射對象
		Map<Long, Boolean> checkinStatusMap = checkinRecordService.getCheckinStatusMap(checkinMap);

		// 5.獲取 標籤 映射對象
		Map<Long, List<Tag>> tagMapByAttendeesId = attendeesTagService
				.getTagMapByAttendeesId(attendeesPage.getRecords());

		// 6.遍歷與會者分頁對象 並組裝VOPage
		List<AttendeesTagVO> attendeesTagVOList = attendeesPage.getRecords().stream().map(attendees -> {
			AttendeesTagVO vo = attendeesConvert.entityToAttendeesTagVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			vo.setCheckinRecordList(checkinMap.getOrDefault(attendees.getAttendeesId(), Collections.emptyList()));
			vo.setIsCheckedIn(checkinStatusMap.getOrDefault(attendees.getAttendeesId(), false));
			vo.setTagList(tagMapByAttendeesId.getOrDefault(attendees.getAttendeesId(), Collections.emptyList()));

			return vo;
		}).toList();

		// 7.返回voList
		return attendeesTagVOList;

	}

	/**
	 * 獲取與會者資訊 和 Tag標籤(分頁)
	 * 
	 * @param pageInfo
	 * @return
	 */
	public IPage<AttendeesTagVO> getAttendeesTagVOPage(Page<Attendees> pageInfo) {

		// 初始化分頁對象
		IPage<AttendeesTagVO> voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize());

		// 1.獲取與會者分頁對象
		IPage<Attendees> attendeesPage = attendeesService.getAttendeesPage(pageInfo);

		// 2.如果查詢的page對象本身就為空,直接返回
		if (attendeesPage.getRecords().isEmpty()) {
			return voPage;
		}

		// 3.組裝AttendeesTagVOList
		List<AttendeesTagVO> attendeesTagVOList = this.buildAttendeesTagVO(attendeesPage);

		// 4.回傳分頁物件
		voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
		voPage.setRecords(attendeesTagVOList);
		return voPage;
	}

	/**
	 * 根據條件參數,獲取所有與會者資訊 和 Tag標籤(分頁)
	 * 
	 * @param pageInfo
	 * @param queryText
	 * @return
	 */
	public IPage<AttendeesTagVO> getAttendeesTagVOPageByQuery(Page<Attendees> pageInfo, String queryText) {

		// 初始化分頁對象
		IPage<AttendeesTagVO> voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize());

		// 1.根據條件查詢符合的會員(與會者的資訊在會員表內)
		List<Member> memberList = memberService.getMembersByQuery(queryText);
		// 如果放上條件查無數據,直接返回
		if (memberList.isEmpty()) {
			return voPage;
		}

		// 2.獲取與會者分頁對象
		IPage<Attendees> attendeesPage = attendeesService.getAttendeesPageByMemberList(pageInfo, memberList);

		// 3.組裝AttendeesTagVOList
		List<AttendeesTagVO> attendeesTagVOList = this.buildAttendeesTagVO(attendeesPage);

		// 4.回傳分頁物件
		voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
		voPage.setRecords(attendeesTagVOList);
		return voPage;

	}

	/**
	 * 為與會者新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param memberId
	 */
	public void assignTagToAttendees(List<Long> targetTagIdList, Long attendeesId) {

		// 1.拿到目標 TagIdSet
		Set<Long> targetTagIdSet = new HashSet<>(targetTagIdList);

		// 2.查詢該attendees所有關聯的tagId Set
		Set<Long> currentTagIdSet = attendeesTagService.getTagIdsByAttendeesId(attendeesId);

		// 3.拿到該移除的集合 和 該新增的集合
		Set<Long> tagsToRemove = Sets.difference(currentTagIdSet, targetTagIdSet);
		Set<Long> tagsToAdd = Sets.difference(targetTagIdSet, currentTagIdSet);

		// 4. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!tagsToRemove.isEmpty()) {
			attendeesTagService.removeTagsFromAttendee(attendeesId, tagsToRemove);
		}

		// 5.執行新增操作
		if (!tagsToAdd.isEmpty()) {
			attendeesTagService.addTagsToAttendees(attendeesId, tagsToAdd);
		}

	}

}
