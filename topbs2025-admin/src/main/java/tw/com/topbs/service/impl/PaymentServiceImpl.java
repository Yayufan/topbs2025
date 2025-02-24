package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaymentConvert;
import tw.com.topbs.mapper.PaymentMapper;
import tw.com.topbs.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Payment;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.PaymentService;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

	private final PaymentConvert paymentConvert;
	private final OrdersService ordersService;

	@Override
	public Payment getPayment(Long paymentId) {
		Payment payment = baseMapper.selectById(paymentId);
		return payment;
	}

	@Override
	public List<Payment> getPaymentList() {
		List<Payment> paymentList = baseMapper.selectList(null);
		return paymentList;
	}

	@Override
	public IPage<Payment> getPaymentPage(Page<Payment> page) {
		Page<Payment> paymentPage = baseMapper.selectPage(page, null);
		return paymentPage;
	}

	@Override
	@Transactional
	public void addPayment(ECPayResponseDTO ECPayResponseDTO) {
		// 轉換綠界金流offical Data 轉換 自己這邊的Entity
		Payment payment = paymentConvert.officalDataToEntity(ECPayResponseDTO);
		// 新增響應回來的交易紀錄
		baseMapper.insert(payment);

		// 如果付款成功，更新訂單的付款狀態
		if (payment.getRtnCode().equals("1")) {
			Orders orders = ordersService.getOrders(payment.getOrdersId());
			// 2 代表付款成功
			orders.setStatus(2);
			ordersService.updateById(orders);
		}else {
			Orders orders = ordersService.getOrders(payment.getOrdersId());
			// 3 代表付款失敗
			orders.setStatus(3);
			ordersService.updateById(orders);
		}

		return;
	}

	@Override
	public void updatePayment(PutPaymentDTO putPaymentDTO) {
		Payment payment = paymentConvert.putDTOToEntity(putPaymentDTO);
		baseMapper.updateById(payment);

	}

	@Override
	public void deletePayment(Long paymentId) {
		baseMapper.deleteById(paymentId);

	}

	@Override
	public void deletePaymentList(List<Long> paymentIds) {
		baseMapper.deleteBatchIds(paymentIds);
	}

}
