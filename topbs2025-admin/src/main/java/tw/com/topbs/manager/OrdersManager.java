package tw.com.topbs.manager;

import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.OrderStatusEnum;
import tw.com.topbs.mapper.OrdersMapper;
import tw.com.topbs.pojo.entity.Orders;

@Component
@RequiredArgsConstructor
public class OrdersManager {

	private final OrdersMapper ordersMapper;

	// 註冊費 和 團體註冊費都屬於 註冊費範疇
	private static final String ITEMS_SUMMARY_REGISTRATION = "Registration Fee";
	private static final String GROUP_ITEMS_SUMMARY_REGISTRATION = "Group Registration Fee";

	public Orders getRegistrationOrderByMemberId(Long memberId) {
		// 找到items_summary 符合 Registration Fee 以及 訂單會員ID與 會員相符的資料
		// 取出status 並放入VO對象中
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.eq(Orders::getMemberId, memberId).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		Orders orders = ordersMapper.selectOne(orderQueryWrapper);
		return orders;
	}

	/**
	 * For Taiwan本國籍的快速審核繳費狀態 (外國團體報名/訂單不在此限)
	 * 修改註冊費繳款狀態 為 付款成功
	 * 
	 * @param memberId
	 * @return
	 */
	public void approveUnpaidMember(Long memberId) {
		// 在訂單表查詢,memberId符合,且ItemSummary 也符合註冊費的訂單
		LambdaQueryWrapper<Orders> ordersWrapper = new LambdaQueryWrapper<>();
		ordersWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getItemsSummary, ITEMS_SUMMARY_REGISTRATION);
		Orders orders = ordersMapper.selectOne(ordersWrapper);

		// 更新訂單付款狀態為 已付款
		orders.setStatus(OrderStatusEnum.PAYMENT_SUCCESS.getValue());

		// 更新進資料庫
		ordersMapper.updateById(orders);
	};

	/**
	 * 獲得註冊費訂單(包含註冊費 和 團體註冊費)列表
	 * 
	 * @return
	 */
	public List<Orders> getRegistrationOrderListForExcel() {

		// 查詢所有沒被刪除 且 items_summary為 註冊費 或者 團體註冊費 訂單
		// 這種名稱在註冊費訂單中只會出現一種，不會同時出現，
		// 也就是註冊費訂單的items_summary 只有 ITEMS_SUMMARY_REGISTRATION 和 GROUP_ITEMS_SUMMARY_REGISTRATION 的選項
		return ordersMapper.selectOrders(ITEMS_SUMMARY_REGISTRATION, GROUP_ITEMS_SUMMARY_REGISTRATION);
	};


}
