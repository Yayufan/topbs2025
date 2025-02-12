package tw.com.topbs.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.OrdersConvert;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.com.topbs.pojo.VO.OrdersVO;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.utils.R;

@Tag(name = "訂單API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/orders")
public class OrdersController {

	private final OrdersService ordersService;
	private final OrdersConvert ordersConvert;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一訂單")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Orders> getOrders(@PathVariable("id") Long ordersId) {
		Orders orders = ordersService.getOrders(ordersId);
		return R.ok(orders);
	}
	
	@GetMapping
	@Operation(summary = "查詢全部訂單")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<OrdersVO>> getUserList() {
		List<Orders> ordersList = ordersService.getOrdersList();
		List<OrdersVO> ordersVOList = ordersConvert.entityListToVOList(ordersList);
		return R.ok(ordersVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部訂單(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<Orders>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Orders> pageable = new Page<Orders>(page,size);
		 IPage<Orders> ordersPage = ordersService.getOrdersPage(pageable);
		return R.ok(ordersPage);
	}
	

	@PostMapping
	@Operation(summary = "新增單一訂單")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Orders> saveOrders(@RequestBody @Valid AddOrdersDTO addOrdersDTO) {
		ordersService.addOrders(addOrdersDTO);
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Operation(summary = "修改訂單")
	public R<Orders> updateOrders(@RequestBody @Valid PutOrdersDTO putOrdersDTO) {
		ordersService.updateOrders(putOrdersDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除訂單")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Orders> deleteOrders(@PathVariable("id") Long ordersId) {
		ordersService.deleteOrders(ordersId);
		return R.ok();
	}
	

	@DeleteMapping
	@Operation(summary = "批量刪除訂單")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeleteOrders(@RequestBody List<Long> ids) {
		ordersService.deleteOrdersList(ids);
		return R.ok();

	}
	
}
