package tw.com.topbs.manager;

import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.OrdersService;

/**
 * 管理會員 和 訂單的需求,<br>
 * 以及成為與會者流程組裝
 */
@Component
@RequiredArgsConstructor
public class MemberOrderManager {

	private final MemberService memberService;
	private final OrdersService ordersService;

	// --------------------------- 查詢相關 ---------------------------------------

	/**
	 * 獲得訂單狀態的會員人數
	 * 
	 * @param status
	 * @return
	 */
	public Integer getMemberOrderCount(Integer status) {

		// 1.查找符合訂單狀態的訂單
		List<Orders> registrationOrderList = ordersService.getRegistrationOrderListByStatus(status);

		// 2.返回當前訂單狀態的會員總人數
		return memberService.getMemberOrderCount(registrationOrderList);

	}

	/**
	 * 獲得會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param status
	 * @param queryText
	 * @return
	 */
	public IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, Integer status, String queryText) {
		// 1.根據分頁 和 訂單狀態, 拿到分頁對象
		Page<Orders> orderPage = ordersService.getRegistrationOrderPageByStatus(page, status);

		// 2.再把訂單分頁 和 會員的查詢條件放入,拿到VO對象並返回
		IPage<MemberOrderVO> memberOrderVO = memberService.getMemberOrderVO(orderPage, status, queryText);
		return memberOrderVO;
	}

	/**
	 * 適用於不使用金流,人工審核<br>
	 * 獲得本國未付款的 會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param queryText
	 * @return
	 */
	public IPage<MemberVO> getUnpaidMemberPage(Page<Member> page, String queryText) {

		// 1.本國籍的快速搜索 (外國團體報名不在此限)
		List<Orders> unpaidRegistrationOrderList = ordersService.getUnpaidRegistrationOrderList();

		// 2.拿到本國籍,未付款的分頁對象
		IPage<MemberVO> unpaidMemberPage = memberService.getUnpaidMemberPage(page, unpaidRegistrationOrderList,
				queryText);
		return unpaidMemberPage;
	}

}
