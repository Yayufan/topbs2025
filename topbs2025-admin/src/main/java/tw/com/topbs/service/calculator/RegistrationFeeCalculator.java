package tw.com.topbs.service.calculator;

import java.math.BigDecimal;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;

//定義價格計算介面
public interface RegistrationFeeCalculator {
	BigDecimal calculateFee(AddMemberDTO addMemberDTO);
}
