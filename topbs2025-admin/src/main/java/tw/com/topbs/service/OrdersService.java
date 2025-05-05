package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.com.topbs.pojo.entity.Orders;

public interface OrdersService extends IService<Orders> {

	Orders getOrders(Long OrdersId);
	
	Orders getOrders(Long memberId,Long OrdersId);
	
	List<Orders> getOrdersList();
	
	List<Orders> getOrdersList(Long memberId);
	
	IPage<Orders> getOrdersPage(Page<Orders> page);
	
	Long addOrders(AddOrdersDTO addOrdersDTO);
	
	void updateOrders(PutOrdersDTO putOrdersDTO);
	
	void updateOrders(Long memberId,PutOrdersDTO putOrdersDTO);
	
	void deleteOrders(Long ordersId);
	
	void deleteOrders(Long memberId,Long ordersId);
	
	void deleteOrdersList(List<Long> OrdersIds);
	
	String payment(Long id);
	
	
}
