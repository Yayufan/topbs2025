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
import tw.com.topbs.convert.OrdersItemConvert;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersItemDTO;
import tw.com.topbs.pojo.VO.OrdersItemVO;
import tw.com.topbs.pojo.entity.OrdersItem;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.OrdersItemService;
import tw.com.topbs.utils.R;

@Tag(name = "訂單明細API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ordersItem")
public class OrdersItemController {

	private final OrdersItemService ordersItemService;
	private final OrdersItemConvert ordersItemConvert;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一訂單明細")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<OrdersItem> getOrdersItem(@PathVariable("id") Long ordersItemId) {
		OrdersItem ordersItem = ordersItemService.getOrdersItem(ordersItemId);
		return R.ok(ordersItem);
	}

	@GetMapping
	@Operation(summary = "查詢全部訂單明細")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<List<OrdersItemVO>> getUserList() {
		List<OrdersItem> ordersItemList = ordersItemService.getOrdersItemList();
		List<OrdersItemVO> ordersItemVOList = ordersItemConvert.entityListToVOList(ordersItemList);
		return R.ok(ordersItemVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部訂單明細(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<IPage<OrdersItem>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<OrdersItem> pageable = new Page<OrdersItem>(page, size);
		IPage<OrdersItem> ordersItemPage = ordersItemService.getOrdersItemPage(pageable);
		return R.ok(ordersItemPage);
	}

	@PostMapping
	@Operation(summary = "新增單一訂單明細")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<OrdersItem> saveOrdersItem(@RequestBody @Valid AddOrdersItemDTO addOrdersItemDTO) {
		ordersItemService.addOrdersItem(addOrdersItemDTO);
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<OrdersItem> updateOrdersItem(@RequestBody @Valid PutOrdersItemDTO putOrdersItemDTO) {
		ordersItemService.updateOrdersItem(putOrdersItemDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Operation(summary = "刪除訂單明細")
	public R<OrdersItem> deleteOrdersItem(@PathVariable("id") Long ordersItemId) {
		ordersItemService.deleteOrdersItem(ordersItemId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除訂單明細")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> batchDeleteOrdersItem(@RequestBody List<Long> ids) {
		ordersItemService.deleteOrdersItemList(ids);
		return R.ok();

	}
}
