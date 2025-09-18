package tw.com.topbs.manager;

import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.OrdersService;

/**
 * 管理會員 和 訂單的需求,以及流程組裝
 */
@Component
@RequiredArgsConstructor
public class MemberOrderManager {

	private final MemberService memberService;
	private final OrdersService ordersService;

	/**
	 * 獲得訂單狀態的會員人數
	 * 
	 * @param status
	 * @return
	 */
	public Integer getMemberOrderCount(Integer status) {
		List<Orders> registrationOrderList = ordersService.getRegistrationOrderListByStatus(status);
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
	IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, Integer status, String queryText) {
		Page<Orders> orderPage = ordersService.getRegistrationOrderPageByStatus(page, status);
		IPage<MemberOrderVO> memberOrderVO = memberService.getMemberOrderVO(orderPage, status, queryText);
		return memberOrderVO;
	}

}
