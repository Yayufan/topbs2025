package tw.com.topbs.manager;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.CheckinRecordConvert;
import tw.com.topbs.enums.CheckinActionTypeEnum;
import tw.com.topbs.exception.CheckinRecordException;
import tw.com.topbs.mapper.CheckinRecordMapper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.entity.CheckinRecord;

@Component
@RequiredArgsConstructor
public class CheckinRecordManager {

	private final CheckinRecordMapper checkinRecordMapper;
	private final CheckinRecordConvert checkinRecordConvert;

	/**
	 * 根據 attendeesId 找到與會者所有簽到/退紀錄
	 * 
	 * @param attendeesId
	 * @return
	 */
	public List<CheckinRecord> getCheckinRecordByAttendeesId(Long attendeesId) {
		// 找到這個與會者所有的checkin紀錄
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);
		List<CheckinRecord> checkinRecordList = checkinRecordMapper.selectList(checkinRecordWrapper);
		return checkinRecordList;
	}

	/**
	 * 根據 attendeesIds 找到對應與會者所有簽到/退紀錄
	 * 
	 * @param attendeesIds
	 * @return
	 */
	public List<CheckinRecord> getCheckinRecordsByAttendeesIds(Collection<Long> attendeesIds) {
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.in(CheckinRecord::getAttendeesId, attendeesIds);
		List<CheckinRecord> checkinRecordList = checkinRecordMapper.selectList(checkinRecordWrapper);
		return checkinRecordList;
	}

	/**
	 * 根據 attendeesIds 創建與會者ID 和 簽到記錄的映射
	 * 
	 * @param attendeesIds
	 * @return
	 */
	public Map<Long, List<CheckinRecord>> getCheckinMapByAttendeesIds(Collection<Long> attendeesIds) {
		List<CheckinRecord> records = this.getCheckinRecordsByAttendeesIds(attendeesIds);
		return records.stream().collect(Collectors.groupingBy(CheckinRecord::getAttendeesId));
	}

	/**
	 * 透過 與會者ID 和 簽到記錄的映射，再創建一個與會者與最後簽到狀態的映射
	 * 
	 * @param checkinMap
	 * @return
	 */
	public Map<Long, Boolean> getCheckinStatusMap(Map<Long, List<CheckinRecord>> checkinMap) {

		// 預定義用來儲存與會者的簽到狀態
		Map<Long, Boolean> statusMap = new HashMap<>();

		//透過Map.entrySet, 獲取key,value 的每次遍歷值
		for (Map.Entry<Long, List<CheckinRecord>> entry : checkinMap.entrySet()) {
			CheckinRecord latest = entry.getValue()
					.stream()
					.max(Comparator.comparing(CheckinRecord::getCheckinRecordId))
					.orElse(null);

			boolean isCheckedIn = latest != null
					&& CheckinActionTypeEnum.CHECKIN.getValue().equals(latest.getActionType());
			statusMap.put(entry.getKey(), isCheckedIn);
		}
		return statusMap;
	}

	/**
	 * 獲取 已簽到 人數
	 * 
	 * @return
	 */
	public Integer getCountCheckedIn() {
		return checkinRecordMapper.countCheckedIn();
	}

	/**
	 * 獲取 尚在現場、已離場 人數
	 * 
	 * @return
	 */
	public PresenceStatsBO getPresenceStats() {
		return checkinRecordMapper.selectPresenceStats();
	}

	public CheckinRecord addCheckinRecord(AddCheckinRecordDTO addCheckinRecordDTO) {

		// 1.查詢指定 AttendeesId 最新的一筆
		CheckinRecord latestRecord = checkinRecordMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
				.eq(CheckinRecord::getAttendeesId, addCheckinRecordDTO.getAttendeesId())
				.orderByDesc(CheckinRecord::getCheckinRecordId)
				.last("LIMIT 1"));

		// 2.如果完全沒資料，代表他沒簽到過， 再判斷此次動作是否為簽退，如果是則拋出異常
		if (latestRecord == null
				&& CheckinActionTypeEnum.CHECKOUT.getValue().equals(addCheckinRecordDTO.getActionType())) {
			throw new CheckinRecordException("沒有簽到記錄，不可簽退");
		}

		// 3.最新數據不為null，判斷是否操作行為一致，如果一致，拋出異常，告知不可連續簽到 或 簽退
		if (latestRecord != null && latestRecord.getActionType().equals(addCheckinRecordDTO.getActionType())) {
			throw new CheckinRecordException("不可連續簽到 或 連續簽退");
		}

		// 4.轉換成entity對象
		CheckinRecord checkinRecord = checkinRecordConvert.addDTOToEntity(addCheckinRecordDTO);
		checkinRecord.setActionTime(LocalDateTime.now());

		// 5.新增進資料庫
		checkinRecordMapper.insert(checkinRecord);

		// 6.返回主鍵ID
		return checkinRecord;
	}

	/**
	 * 根據attendeesId , 找到這位與會者簡易的簽到退紀錄
	 * <p>
	 * (已最早的簽到紀錄 和 最晚的簽退紀錄組成)
	 * 
	 * @param attendeesId
	 * @return
	 */
	public CheckinInfoBO getLastCheckinRecordByAttendeesId(Long attendeesId) {
		// 先找到這個與會者所有的checkin紀錄
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);
		List<CheckinRecord> checkinRecordList = checkinRecordMapper.selectList(checkinRecordWrapper);

		// 創建簡易簽到/退紀錄的BO對象
		CheckinInfoBO checkinInfoBO = new CheckinInfoBO();
		LocalDateTime checkinTime = null;
		LocalDateTime checkoutTime = null;

		// 遍歷所有簽到/退紀錄
		for (CheckinRecord record : checkinRecordList) {
			// 如果此次紀錄為 '簽到'
			if (record.getActionType() == 1) {
				// 在簽到時間為null 或者 遍歷對象的執行時間 早於 當前簽到時間的數值
				if (checkinTime == null || record.getActionTime().isBefore(checkinTime)) {
					// checkinTime的值進行覆蓋
					checkinTime = record.getActionTime();
				}
				// 如果此次紀錄為 '簽退'
			} else if (record.getActionType() == 2) {
				// 在簽到時間為null 或者 遍歷對象的執行時間 晚於 當前簽退時間的數值
				if (checkoutTime == null || record.getActionTime().isAfter(checkoutTime)) {
					checkoutTime = record.getActionTime();
				}
			}
		}

		// 將最早的簽到時間 和 最晚的簽退時間,組裝到BO對象中
		checkinInfoBO.setCheckinTime(checkinTime);
		checkinInfoBO.setCheckoutTime(checkoutTime);

		return checkinInfoBO;
	}

	/**
	 * 根據 attendeesId 刪除對應的與會者簽到記錄
	 * 
	 * @param attendeesId
	 */
	public void deleteCheckinRecordByAttendeesId(Long attendeesId) {
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);

		checkinRecordMapper.delete(checkinRecordWrapper);
	}

}
