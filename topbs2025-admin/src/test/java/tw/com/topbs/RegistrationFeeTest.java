package tw.com.topbs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.topbs.config.RegistrationFeeConfig;
import tw.com.topbs.enums.MemberCategoryEnum;

@SpringBootTest
public class RegistrationFeeTest {
	
	@Autowired
	private RegistrationFeeConfig registrationFeeConfig;

	@Test
	public void printAllFees() {
		
		Map<String, Map<String, Map<String, Long>>> fees = registrationFeeConfig.getRegistrationFee();
		assertNotNull(fees, "Fees should not be null");

		// 1.第一次遍歷,拿到階段 和 國籍Map
		fees.forEach((phase, countryMap) -> {
			System.out.println("=== Phase: " + phase + " ===");
			
			// 2.第二次遍歷,拿到國籍 和 身分Map
			countryMap.forEach((country, categoryMap) -> {
				System.out.println(" Country: " + country);
				
				// 3.第三次遍歷,遍歷Enum的身分類別,拿到configKey 以此達到amount
				for (MemberCategoryEnum categoryEnum : MemberCategoryEnum.values()) {
					String key = categoryEnum.getConfigKey();
					Long amount = categoryMap.get(key);
					if (amount == null) {
						System.out.println("  [WARN] Missing category: " + key);
						continue;
					}
					BigDecimal fee = BigDecimal.valueOf(amount);
					System.out.println("  " + key + ": " + fee);
				}
			});
		});
	}
}
