package tw.com.topbs.manager;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.OrdersItemMapper;
import tw.com.topbs.pojo.entity.OrdersItem;

@Component
@RequiredArgsConstructor
public class OrdersItemManager {

	private static final String ITEMS_SUMMARY_REGISTRATION = "Registration Fee";
	
	private final OrdersItemMapper ordersItemMapper;

	public void addRegistrationOrderItem(Long orderId, BigDecimal amount) {
		OrdersItem ordersItem = new OrdersItem();
		ordersItem.setOrdersId(orderId);
		ordersItem.setProductType(ITEMS_SUMMARY_REGISTRATION);
		ordersItem.setProductName("2025 TICBCS Registration Fee");
		ordersItem.setUnitPrice(amount);
		ordersItem.setQuantity(1);
		ordersItem.setSubtotal(amount.multiply(BigDecimal.ONE));

		ordersItemMapper.insert(ordersItem);
	}

}
