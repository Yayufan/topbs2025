package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaymentDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.com.topbs.pojo.VO.PaymentVO;
import tw.com.topbs.pojo.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentConvert {

	Payment addDTOToEntity(AddPaymentDTO addPaymentDTO);

	Payment putDTOToEntity(PutPaymentDTO putPaymentDTO);
	
	PaymentVO entityToVO(Payment payment);
	
	List<PaymentVO> entityListToVOList(List<Payment> paymentList);
}
