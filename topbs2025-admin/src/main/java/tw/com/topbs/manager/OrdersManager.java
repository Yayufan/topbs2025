package tw.com.topbs.manager;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

	/**
	 * 根據繳費狀態,查詢符合的註冊費訂單(註冊費 和 團體註冊費)列表
	 * 
	 * @param status
	 * @return
	 */
	public List<Orders> getRegistrationOrderListByStatus(Integer status) {
		// 查找itemsSummary 為 註冊費 , 以及符合status 的member數量
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.eq(status != null, Orders::getStatus, status).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		List<Orders> orderList = ordersMapper.selectList(orderQueryWrapper);
		return orderList;

	}

	/**
	 * 根據繳費狀態,查詢符合的註冊費訂單(註冊費 和 團體註冊費)分頁對象
	 * 
	 * @param page
	 * @param status
	 * @return
	 */
	public Page<Orders> getRegistrationOrderPageByStatus(Page<Orders> page, Integer status) {
		LambdaQueryWrapper<Orders> orderQueryWrapper = new LambdaQueryWrapper<>();
		orderQueryWrapper.eq(status != null, Orders::getStatus, status).and(wrapper -> {
			wrapper.eq(Orders::getItemsSummary, ITEMS_SUMMARY_REGISTRATION)
					.or()
					.eq(Orders::getItemsSummary, GROUP_ITEMS_SUMMARY_REGISTRATION);
		});

		Page<Orders> ordersPage = ordersMapper.selectPage(page, orderQueryWrapper);

		return ordersPage;

	};

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
	 * For Taiwan本國籍的快速搜索 (外國團體報名不在此限)
	 * 查詢尚未付款，ItemSummary為註冊費的訂單資料；
	 *
	 * @return
	 */
	public List<Orders> getUnpaidRegistrationOrderList() {
		// 
		LambdaQueryWrapper<Orders> ordersWrapper = new LambdaQueryWrapper<>();
		ordersWrapper.eq(Orders::getStatus, OrderStatusEnum.UNPAID.getValue())
				.eq(Orders::getItemsSummary, ITEMS_SUMMARY_REGISTRATION);
		List<Orders> ordersList = ordersMapper.selectList(ordersWrapper);

		return ordersList;

	};

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
		List<Orders> orderList = ordersMapper.selectOrders(ITEMS_SUMMARY_REGISTRATION,
				GROUP_ITEMS_SUMMARY_REGISTRATION);

		return orderList;
	};
	
	
	/**
	 * 註冊費0元的訂單, 之後要換成符合金流的
	 * 
	 * @param memberId
	 * @return
	 */
	public Orders createZeroAmountRegistrationOrder(Long memberId) {
		//此為0元訂單
		BigDecimal amount = BigDecimal.ZERO;

		//創建繳費完成的註冊費訂單
		Orders orders = new Orders();
		orders.setMemberId(memberId);
		orders.setItemsSummary(ITEMS_SUMMARY_REGISTRATION);
		orders.setStatus(OrderStatusEnum.PAYMENT_SUCCESS.getValue());
		orders.setTotalAmount(amount);

		// 資料庫新增
		ordersMapper.insert(orders);

		return orders;

	}

}
