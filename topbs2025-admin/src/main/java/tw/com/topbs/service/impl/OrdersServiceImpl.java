package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.OrdersConvert;
import tw.com.topbs.mapper.OrdersMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.service.OrdersItemService;
import tw.com.topbs.service.OrdersService;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

	private final OrdersConvert ordersConvert;
	private final OrdersItemService ordersItemService;

	@Override
	public Orders getOrders(Long ordersId) {
		Orders orders = baseMapper.selectById(ordersId);
		return orders;
	}

	@Override
	public List<Orders> getOrdersList() {
		List<Orders> ordersList = baseMapper.selectList(null);
		return ordersList;
	}

	@Override
	public IPage<Orders> getOrdersPage(Page<Orders> page) {
		Page<Orders> ordersPage = baseMapper.selectPage(page, null);
		return ordersPage;
	}

	@Override
	@Transactional
	public Long addOrders(AddOrdersDTO addOrdersDTO) {
		//新增訂單本身
		Orders orders = ordersConvert.addDTOToEntity(addOrdersDTO);
		baseMapper.insert(orders);

		return orders.getOrdersId();
	}

	@Override
	public void updateOrders(PutOrdersDTO putOrdersDTO) {
		Orders orders = ordersConvert.putDTOToEntity(putOrdersDTO);
		baseMapper.updateById(orders);
	}

	@Override
	public void deleteOrders(Long ordersId) {
		baseMapper.deleteById(ordersId);
	}

	@Override
	public void deleteOrdersList(List<Long> ordersIds) {
		baseMapper.deleteBatchIds(ordersIds);
	}

}
