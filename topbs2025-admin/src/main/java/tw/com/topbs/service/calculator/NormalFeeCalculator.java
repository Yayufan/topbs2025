package tw.com.topbs.service.calculator;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.exception.RegistrationInfoException;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.utils.CountryUtil;

@Component("normalFeeCalculator")
public class NormalFeeCalculator implements RegistrationFeeCalculator {
	@Override
	public BigDecimal calculateFee(AddMemberDTO addMemberDTO) {

		MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(addMemberDTO.getCategory());
		
		if (CountryUtil.isNational(addMemberDTO.getCountry())) {
			return switch (memberCategoryEnum) {
			case MEMBER -> BigDecimal.valueOf(1000L); // Member(會員)
			case OTHERS -> BigDecimal.valueOf(1200L); // Others(學生或護士)
			case NON_MEMBER -> BigDecimal.valueOf(1500L); // Non-Member(非會員)
			default -> throw new RegistrationInfoException("category is not in system");
			};
		} else {
			return switch (memberCategoryEnum) {
			case MEMBER -> BigDecimal.valueOf(12800L); // Member
			case OTHERS -> BigDecimal.valueOf(6400L); // Others
			case NON_MEMBER -> BigDecimal.valueOf(16000L); // Non-member
			default -> throw new RegistrationInfoException("category is not in system");
			};
		}
	}
}