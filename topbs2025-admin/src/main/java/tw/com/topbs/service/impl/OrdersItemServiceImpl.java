package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.OrdersItemConvert;
import tw.com.topbs.mapper.OrdersItemMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.com.topbs.pojo.entity.OrdersItem;
import tw.com.topbs.service.OrdersItemService;

@Service
@RequiredArgsConstructor
public class OrdersItemServiceImpl extends ServiceImpl<OrdersItemMapper, OrdersItem> implements OrdersItemService {

	private final OrdersItemConvert ordersItemConvert;

	@Override
	public OrdersItem getOrdersItem(Long ordersItemId) {
		OrdersItem ordersItem = baseMapper.selectById(ordersItemId);
		return ordersItem;
	}

	@Override
	public List<OrdersItem> getOrdersItemList() {
		List<OrdersItem> ordersItemList = baseMapper.selectList(null);
		return ordersItemList;
	}

	@Override
	public IPage<OrdersItem> getOrdersItemPage(Page<OrdersItem> page) {
		Page<OrdersItem> ordersItemPage = baseMapper.selectPage(page, null);
		return ordersItemPage;
	}

	@Override
	public void addOrdersItem(AddOrdersItemDTO addOrdersItemDTO) {
		OrdersItem ordersItem = ordersItemConvert.addDTOToEntity(addOrdersItemDTO);
		baseMapper.insert(ordersItem);
	}

	@Override
	public void updateOrdersItem(PutOrdersItemDTO putOrdersItemDTO) {
		OrdersItem ordersItem = ordersItemConvert.putDTOToEntity(putOrdersItemDTO);
		baseMapper.updateById(ordersItem);

	}

	@Override
	public void deleteOrdersItem(Long ordersItemId) {
		baseMapper.deleteById(ordersItemId);
	}

	@Override
	public void deleteOrdersItemList(List<Long> ordersItemIds) {
		baseMapper.deleteBatchIds(ordersItemIds);
	}

}
