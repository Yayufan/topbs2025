package tw.com.topbs.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import ecpay.payment.integration.AllInOne;
import ecpay.payment.integration.domain.AioCheckOutOneTime;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.OrdersConvert;
import tw.com.topbs.exception.OrderPaymentException;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.mapper.OrdersMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutOrdersDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.service.OrdersItemService;
import tw.com.topbs.service.OrdersService;

@Service
@RequiredArgsConstructor
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements OrdersService {

	private static final AtomicInteger counter = new AtomicInteger(0);

	private final OrdersConvert ordersConvert;
	private final OrdersItemService ordersItemService;
	private final MemberMapper memberMapper;

	@Override
	public Orders getOrders(Long ordersId) {
		Orders orders = baseMapper.selectById(ordersId);
		return orders;
	}

	@Override
	public Orders getOrders(Long memberId, Long ordersId) {
		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getOrdersId, ordersId);

		Orders orders = baseMapper.selectOne(ordersQueryWrapper);

		return orders;
	}

	@Override
	public List<Orders> getOrdersList() {
		List<Orders> ordersList = baseMapper.selectList(null);
		return ordersList;
	}

	@Override
	public List<Orders> getOrdersList(Long memberId) {
		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId);
		List<Orders> ordersList = baseMapper.selectList(ordersQueryWrapper);
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
		// 新增訂單本身
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
	public void updateOrders(Long memberId, PutOrdersDTO putOrdersDTO) {
		Orders orders = ordersConvert.putDTOToEntity(putOrdersDTO);

		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getOrdersId, orders.getOrdersId());
		baseMapper.update(orders, ordersQueryWrapper);
	}

	@Override
	public void deleteOrders(Long ordersId) {
		baseMapper.deleteById(ordersId);
	}

	@Override
	public void deleteOrders(Long memberId, Long ordersId) {
		LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
		ordersQueryWrapper.eq(Orders::getMemberId, memberId).eq(Orders::getOrdersId, ordersId);
		baseMapper.delete(ordersQueryWrapper);
	}

	@Override
	public void deleteOrdersList(List<Long> ordersIds) {
		baseMapper.deleteBatchIds(ordersIds);
	}

	@Override
	public String payment(Long id) {
		// 創建全方位金流對象
		AllInOne allInOne = new AllInOne("");

		// 創建信用卡一次付清模式
		AioCheckOutOneTime aioCheckOutOneTime = new AioCheckOutOneTime();

		// 根據前端傳來的資料,獲取訂單
		Orders orders = this.getOrders(id);

		// 根據訂單ID,獲取這個訂單的持有者Member，如果訂單為子報名者要求產生，則直接拋出錯誤
		Member member = memberMapper.selectById(orders.getMemberId());
		if (member.getGroupRole() == "slave") {
			throw new OrderPaymentException("Group registration must be paid by the primary registrant");
		}

		// 獲取當前時間並格式化，為了填充交易時間
		LocalDateTime now = LocalDateTime.now();
		String nowFormat = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));

		// 訂單交易編號,僅接受20位長度，編號不可重複，使用自定義生成function 處理
		aioCheckOutOneTime.setMerchantTradeNo(this.generateTradeNo());

		// 設定交易日期
		aioCheckOutOneTime.setMerchantTradeDate(nowFormat);

		// 綠界金流 僅接受新台幣 以及整數的金額，所以BigDecimal 要進行去掉無意義的0以及轉換成String
		aioCheckOutOneTime.setTotalAmount(orders.getTotalAmount().stripTrailingZeros().toPlainString());

		// 設定交易描述
		aioCheckOutOneTime.setTradeDesc(
				"This payment page only displays the total order amount. For details, please see the TOPBS2025 official website membership page, TOPPS 2025 registration fee");
		// 設定交易產品名稱概要,他沒有辦法一個item對應一個amount , 但可以透過#將item分段顯示
		// 例如: item01#item02#item03
		aioCheckOutOneTime.setItemName(orders.getItemsSummary());

		// 設定付款完成後，返回的前端網址，這邊讓他回到官網
		aioCheckOutOneTime.setClientBackURL("https://topbs.zfcloud.cc/member");
		// 設定付款完成通知的網址,應該可以直接設定成後端API，實證有效
		aioCheckOutOneTime.setReturnURL("https://topbs.zfcloud.cc/prod-api/payment");
		// 這邊不需要他回傳額外付款資料
		aioCheckOutOneTime.setNeedExtraPaidInfo("N");
		// 設定英文介面
		aioCheckOutOneTime.setLanguage("ENG");

		// 這邊使用他預留的客製化欄位,填入我們的訂單ID,當他透過return URL 觸發我們API時會回傳
		// 這邊因為還是只能String , 所以要將Long 類型做轉換
		aioCheckOutOneTime.setCustomField1(String.valueOf(orders.getOrdersId()));

		String form = allInOne.aioCheckOut(aioCheckOutOneTime, null);
		System.out.println("產生的form " + form);
		return form;

	}

	private String generateTradeNo() {
		// 獲取UTC當前時間戳
		long timestamp = System.currentTimeMillis();
		// 每次請求自增，並限制在 0~99 之間
		int count = counter.getAndIncrement() % 100;
		// 最後開頭用topbs + 時間戳 + 自增數
		return "topbs" + timestamp + String.format("%02d", count); // 生成交易编号
	}

}
