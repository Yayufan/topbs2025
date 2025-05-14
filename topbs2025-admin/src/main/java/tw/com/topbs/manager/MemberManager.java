package tw.com.topbs.manager;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.entity.Member;

@Component
@RequiredArgsConstructor
public class MemberManager {

	private final MemberMapper memberMapper;

	public Member getMemberById(Long memberId) {
		LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
		memberWrapper.eq(Member::getMemberId, memberId);
		Member member = memberMapper.selectOne(memberWrapper);
		return member;
	}

	public List<Member> getAllMembersEfficiently() {
		List<Member> memberList = memberMapper.selectMembers();
		return memberList;
	}

	public List<Member> getMembersByIds(Collection<Long> memberIds) {
		List<Member> memberList = memberMapper.selectBatchIds(memberIds);
		return memberList;
	}

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

}
