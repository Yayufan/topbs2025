package tw.com.topbs.convert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.com.topbs.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaymentDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.com.topbs.pojo.VO.PaymentVO;
import tw.com.topbs.pojo.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentConvert {

	@Mapping(target = "ordersId", source = "customField1")
	@Mapping(target = "paymentDate", source = "paymentDate", qualifiedByName = "stringToLocalDateTime")
	@Mapping(target = "tradeDate", source = "tradeDate", qualifiedByName = "stringToLocalDateTime")
	Payment officalDataToEntity(ECPayResponseDTO ecPayResponseDTO);

	Payment addDTOToEntity(AddPaymentDTO addPaymentDTO);

	Payment putDTOToEntity(PutPaymentDTO putPaymentDTO);

	PaymentVO entityToVO(Payment payment);

	List<PaymentVO> entityListToVOList(List<Payment> paymentList);

	@Named("stringToLocalDateTime")
	default LocalDateTime stringToLocalDateTime(String date) {
		if (date == null || date.isEmpty()) {
			return null;
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		return LocalDateTime.parse(date, formatter);

	}

}
