package tw.com.topbs.manager;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.CheckinRecordMapper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.entity.CheckinRecord;

@Component
@RequiredArgsConstructor
public class CheckinRecordManager {

	private final CheckinRecordMapper checkinRecordMapper;

	
	public CheckinInfoBO getLastCheckinRecordByAttendeesId(Long attendeesId) {
		// 先找到這個與會者所有的checkin紀錄
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);
		List<CheckinRecord> checkinRecordList = checkinRecordMapper.selectList(checkinRecordWrapper);

		CheckinInfoBO checkinInfoBO = new CheckinInfoBO();
		LocalDateTime checkinTime = null;
		LocalDateTime checkoutTime = null;

		for (CheckinRecord record : checkinRecordList) {
			if (record.getActionType() == 1) {
				if (checkinTime == null || record.getActionTime().isBefore(checkinTime)) {
					checkinTime = record.getActionTime();
				}
			} else if (record.getActionType() == 2) {
				if (checkoutTime == null || record.getActionTime().isAfter(checkoutTime)) {
					checkoutTime = record.getActionTime();
				}
			}
		}

		checkinInfoBO.setCheckinTime(checkinTime);
		checkinInfoBO.setCheckoutTime(checkoutTime);

		return checkinInfoBO;
	}

}
