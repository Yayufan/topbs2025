package tw.com.topbs.pojo.BO;

import lombok.Data;

@Data
public class PresenceStatsBO {
	/**
	 * 尚在會場 人數
	 */
	private Integer totalOnsite;
	
	/**
	 * 已離場 人數
	 */
	private Integer totalLeft;
}
