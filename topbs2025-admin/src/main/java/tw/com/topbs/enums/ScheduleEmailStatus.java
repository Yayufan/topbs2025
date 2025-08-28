package tw.com.topbs.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ScheduleEmailStatus {

	PENDING(0, "待執行"),
	EXECUTE(1, "執行中"), 
	FINISHED(2, "執行完成"), 
	FAILED(3, "處理失敗"), 
	CANCELED(4, "取消執行");

	private final Integer value;
	private final String label;

	public static ScheduleEmailStatus fromValue(Integer value) {
		for (ScheduleEmailStatus type : values()) {
			if (type.value.equals(value))
				return type;
		}
		throw new IllegalArgumentException("無效的 排程信件任務 狀態類型值: " + value);
	}

}
