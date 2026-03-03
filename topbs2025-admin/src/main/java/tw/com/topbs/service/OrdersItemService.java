package tw.com.topbs.service;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.OrdersItem;

public interface OrdersItemService extends IService<OrdersItem> {

	/**
	 * 現場註冊者, 立刻產生他的免費訂單
	 * 
	 * @param orderId
	 * @param amount
	 */
	void addRegistrationOrderItem(Long orderId, BigDecimal amount);

	/**
	 * 創建 註冊費訂單的 訂單細項
	 * 
	 * @param order
	 */
	void createRegistrationOrderItem(Orders order);
	
	/**
	 * 創建 團體報名 註冊費訂單的 訂單細項
	 * @param order
	 */
	void createGroupRegistrationOrderItem(Orders order);

	OrdersItem getOrdersItem(Long oredersItemId);

	List<OrdersItem> getOrdersItemList();

	IPage<OrdersItem> getOrdersItemPage(Page<OrdersItem> page);

	void addOrdersItem(AddOrdersItemDTO addOrdersItemDTO);

	void updateOrdersItem(PutOrdersItemDTO putOrdersItemDTO);
	
	void deleteOrdersItem(Long oredersItemId);

	void deleteOrdersItemList(List<Long> oredersItemIds);
	
	/**
	 * 根據訂單ID , 刪除其訂單細項
	 * 
	 * @param orderId
	 */
	void deleteOrdersItemByOrderId(Long orderId);

}
