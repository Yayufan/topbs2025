package tw.com.topbs.service;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;

public interface OrdersService extends IService<Orders> {

	/**
	 * 根據繳費狀態,查詢符合的註冊費訂單(註冊費 和 團體註冊費)分頁對象
	 * 
	 * @param page
	 * @param status
	 * @return
	 */
	Page<Orders> getRegistrationOrderPageByStatus(Page<Orders> page, Integer status);

	/**
	 * 根據繳費狀態,查詢符合的註冊費訂單(註冊費 和 團體註冊費)列表
	 * 
	 * @param status
	 * @return
	 */
	List<Orders> getRegistrationOrderListByStatus(Integer status);

	/**
	 * 找到會員的註冊費訂單
	 * 
	 * @param memberId
	 * @return
	 */
	Orders getRegistrationOrderByMemberId(Long memberId);

	/**
	 * For Taiwan本國籍的快速搜索 (外國團體報名不在此限)
	 * 查詢尚未付款，ItemSummary為註冊費的訂單資料；
	 *
	 * @return
	 */
	List<Orders> getUnpaidRegistrationOrderList();

	/**
	 * For Taiwan本國籍的快速審核繳費狀態 (外國團體報名/訂單不在此限)
	 * 修改註冊費繳款狀態 為 付款成功
	 * 
	 * @param memberId
	 * @return
	 */
	void approveUnpaidMember(Long memberId);

	/**
	 * 獲得註冊費訂單(包含註冊費 和 團體註冊費)列表
	 * 
	 * @return
	 */
	List<Orders> getRegistrationOrderListForExcel();

	/**
	 * 創建註冊費訂單<br>
	 * 付款狀態為 「未付款」
	 * 
	 * @param amount
	 * @param member
	 */
	void createRegistrationOrder(BigDecimal amount, Member member);

	/**
	 * 創建 「免費」 註冊費訂單<br>
	 * 付款狀態為 「已付款」<br>
	 * 主要適用於MVP、Speaker、Moderator
	 * 
	 * @param member
	 */
	void createFreeRegistrationOrder(Member member);

	/**
	 * 創建 團體報名 註冊費訂單<br>
	 * 付款狀態為 「未付款」
	 * 
	 * @param amount
	 * @param member
	 */
	void createGroupRegistrationOrder(BigDecimal amount, Member member);

	/**
	 * 創建 「免費」 團體報名註冊費訂單<br>
	 * 付款狀態為 「未付款」
	 * 
	 * @param member
	 */
	void createFreeGroupRegistrationOrder(Member member);

	/**
	 * 註冊費0元的訂單, 之後要換成符合金流的
	 * 
	 * @param memberId
	 * @return
	 */
	Orders createZeroAmountRegistrationOrder(Long memberId);

	Orders getOrders(Long OrdersId);

	Orders getOrders(Long memberId, Long OrdersId);

	List<Orders> getOrdersList();

	List<Orders> getOrdersList(Long memberId);

	IPage<Orders> getOrdersPage(Page<Orders> page);

	Long addOrders(AddOrdersDTO addOrdersDTO);

	void updateOrders(PutOrdersDTO putOrdersDTO);

	void updateOrders(Long memberId, PutOrdersDTO putOrdersDTO);

	void deleteOrders(Long ordersId);

	void deleteOrders(Long memberId, Long ordersId);

	void deleteOrdersList(List<Long> OrdersIds);

	String payment(Long id);

}
