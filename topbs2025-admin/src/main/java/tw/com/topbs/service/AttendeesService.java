package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
public interface AttendeesService extends IService<Attendees> {

	/**
	 * 拿到與會者分組 index
	 * 
	 * @param groupSize
	 * @return
	 */
	int getAttendeesGroupIndex(int groupSize);

	Attendees getAttendees(Long attendeesId);

	List<Attendees> getAttendeesList();

	/**
	 * mybatis 原始高速查詢所有Attendees<br>
	 * 輸出Excel數據適用
	 * 
	 * @return
	 */
	List<Attendees> getAttendeesEfficiently();

	/**
	 * 根據ids查詢符合的與會者
	 * 
	 * @param attendeesIds
	 * @return
	 */
	List<Attendees> getAttendeesListByIds(Collection<Long> attendeesIds);

	IPage<Attendees> getAttendeesPage(Page<Attendees> page);

	/**
	 * 查詢符合memberList範圍內的與會者
	 * 
	 * @param page
	 * @param memberList
	 * @return
	 */
	IPage<Attendees> getAttendeesPageByMemberList(Page<Attendees> page, Collection<Member> memberList);

	/**
	 * 根據會員資訊 建立 與會者
	 * 
	 * @param member
	 * @return
	 */
	Attendees addAttendees(Member member);

	void deleteAttendees(Long attendeesId);

	/**
	 * 根據memberId 刪除與會者身分
	 * 
	 * @param memberId
	 */
	Attendees deleteAttendeesByMemberId(Long memberId);

	/**
	 * 高效獲取所有與會者的映射對象
	 * 
	 * @return 以attendeesId為key , Attendees 為值的value
	 */
	Map<Long, Attendees> getAttendeesMap();

	/**
	 * 查詢應簽到人數
	 * 
	 * @return
	 */
	Integer countTotalShouldAttend();

}
