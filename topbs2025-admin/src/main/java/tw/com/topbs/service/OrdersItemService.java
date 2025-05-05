package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.com.topbs.pojo.entity.OrdersItem;

public interface OrdersItemService extends IService<OrdersItem> {

	OrdersItem getOrdersItem(Long oredersItemId);
	
	List<OrdersItem> getOrdersItemList();
	
	IPage<OrdersItem> getOrdersItemPage(Page<OrdersItem> page);
	
	void addOrdersItem(AddOrdersItemDTO addOrdersItemDTO);
	
	void updateOrdersItem(PutOrdersItemDTO putOrdersItemDTO);
	
	void deleteOrdersItem(Long oredersItemId);
	
	void deleteOrdersItemList(List<Long> oredersItemIds);
	
}
