package tw.com.topbs.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

/**
 * 在project底下的 registration-fee 單獨抽出<br>
 * 因為比較複雜
 * 
 */

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "project")
public class RegistrationFeeConfig {

	/**
	 * 
	 * 階段 -> 國籍 -> 類別 -> 金額<br>
	 * Map<phase, Map<country, Map<category, amount>>>
	 * 
	 */
	private Map<String, Map<String, Map<String, Long>>> registrationFee  ;
	

	/** 取得指定階段/國籍/身份的金額 */
	public BigDecimal getFee(String phase, String country, String category) {
		Map<String, Map<String, Long>> countryMap = registrationFee.get(phase);
		if (countryMap == null) {
			throw new IllegalArgumentException("Phase not configured: " + phase);
		}
		Map<String, Long> categoryMap = countryMap.get(country);
		if (categoryMap == null) {
			throw new IllegalArgumentException("Country not configured: " + country);
		}
		Long amount = categoryMap.get(category);
		if (amount == null) {
			throw new IllegalArgumentException("Category not configured: " + category);
		}
		return BigDecimal.valueOf(amount);
	}

}
