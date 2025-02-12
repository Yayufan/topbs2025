package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaymentConvert;
import tw.com.topbs.mapper.PaymentMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaymentDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.com.topbs.pojo.entity.Payment;
import tw.com.topbs.service.PaymentService;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl  extends ServiceImpl<PaymentMapper, Payment>  implements PaymentService {
	
	private final PaymentConvert paymentConvert;
	
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
	public void addPayment(AddPaymentDTO addPaymentDTO) {
		Payment payment = paymentConvert.addDTOToEntity(addPaymentDTO);
		baseMapper.insert(payment);
		return ;
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
