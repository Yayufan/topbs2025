package tw.com.topbs.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.exception.RegisteredAlreadyExistsException;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.DTO.WalkInRegistrationDTO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.CheckinRecordService;
import tw.com.topbs.service.MemberService;

@Component
@RequiredArgsConstructor
public class MemberManager {

	private final MemberService memberService;
	private final AttendeesService attendeesService;
	private final CheckinRecordService checkinRecordService;

	private final MemberMapper memberMapper;

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

		// 2.刪除他的簽到/退紀錄
		checkinRecordService.deleteCheckinRecordByAttendeesId(attendees.getAttendeesId());

		// 3.最後刪除自身
		memberService.deleteMember(memberId);

	}

	/**
	 * 批量刪除會員<br>
	 * 包含其與會者身分 和 簽到退紀錄
	 * 
	 * @param memberIds
	 */
	@Transactional
	public void deleteMemberList(List<Long> memberIds) {
		for (Long memberId : memberIds) {
			this.deleteMember(memberId);
		}

	}

	// ------------------------- 以下為舊資料,重構後會刪除 -----------------------------------

	/**
	 * 根據會員ID,獲取會員
	 * 
	 * @param memberId
	 * @return
	 */
	public Member getMemberById(Long memberId) {
		LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
		memberWrapper.eq(Member::getMemberId, memberId);
		Member member = memberMapper.selectOne(memberWrapper);
		return member;
	}

	/**
	 * 透過mybatis , 高效查詢
	 * 
	 * @return
	 */
	public List<Member> getAllMembersEfficiently() {
		List<Member> memberList = memberMapper.selectMembers();
		return memberList;
	}

	/**
	 * 透過memberIds 查詢會員列表
	 * 
	 * @param memberIds
	 * @return
	 */
	public List<Member> getMembersByIds(Collection<Long> memberIds) {
		List<Member> memberList = memberMapper.selectBatchIds(memberIds);
		return memberList;
	}

	/**
	 * 透過memberIds 查詢會員ID-會員映射
	 * 
	 * @param memberIds
	 * @return
	 */
	public Map<Long, Member> getMemberMapByIds(Collection<Long> memberIds) {
		//如果memberIds為空，直接返回空Map
		if (memberIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 訪問本地方法，查詢memberList，並做映射轉換
		List<Member> memberList = this.getMembersByIds(memberIds);
		return memberList.stream().collect(Collectors.toMap(Member::getMemberId, Function.identity()));
	};

	/**
	 * 根據搜尋條件查詢會員列表
	 * 
	 * @param queryText
	 * @return
	 */
	public List<Member> getMembersByQuery(String queryText) {
		// 1.因為能進與會者其實沒有單獨的資訊了，所以是查詢會員資訊，queryText都是member的資訊
		LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();

		// 當 queryText 不為空字串、空格字串、Null 時才加入篩選條件
		// 且attendeesIdsByStatus裡面元素不為空，則加入篩選條件
		memberWrapper.and(StringUtils.isNotBlank(queryText),
				wrapper -> wrapper.like(Member::getChineseName, queryText)
						.or()
						.like(Member::getFirstName, queryText)
						.or()
						.like(Member::getLastName, queryText)
						.or()
						.like(Member::getPhone, queryText)
						.or()
						.like(Member::getIdCard, queryText)
						.or()
						.like(Member::getEmail, queryText));

		List<Member> memberList = memberMapper.selectList(memberWrapper);

		return memberList;

	}

	/**
	 * 查詢當前會員總數
	 * 
	 * @return
	 */
	public Long getMemberCount() {
		return memberMapper.selectCount(null);
	};

	/**
	 * 現場報到時，新增會員
	 * 
	 * @param walkInRegistrationDTO
	 * @return
	 */
	public Member addMemberOnSite(WalkInRegistrationDTO walkInRegistrationDTO) {

		Member member = new Member();
		member.setEmail(walkInRegistrationDTO.getEmail());
		member.setChineseName(walkInRegistrationDTO.getChineseName());
		member.setFirstName(walkInRegistrationDTO.getFirstName());
		member.setLastName(walkInRegistrationDTO.getLastName());
		member.setCategory(walkInRegistrationDTO.getCategory());

		//判斷Email有無被註冊過
		LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		memberQueryWrapper.eq(Member::getEmail, member.getEmail());
		Long memberCount = memberMapper.selectCount(memberQueryWrapper);

		if (memberCount > 0) {
			throw new RegisteredAlreadyExistsException("This E-Mail has been registered");
		}

		memberMapper.insert(member);

		return member;
	};

}
