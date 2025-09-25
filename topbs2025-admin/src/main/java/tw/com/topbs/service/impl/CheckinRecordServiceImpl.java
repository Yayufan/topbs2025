package tw.com.topbs.service.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.CheckinRecordConvert;
import tw.com.topbs.enums.CheckinActionTypeEnum;
import tw.com.topbs.exception.CheckinRecordException;
import tw.com.topbs.mapper.CheckinRecordMapper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.service.CheckinRecordService;

/**
 * <p>
 * 簽到退紀錄 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Service
@RequiredArgsConstructor
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord>
		implements CheckinRecordService {

	private final CheckinRecordConvert checkinRecordConvert;

	@Override
	public CheckinRecord getCheckinRecord(Long checkinRecordId) {
		return baseMapper.selectById(checkinRecordId);
	}

	@Override
	public List<CheckinRecord> getCheckinRecordList() {
		return baseMapper.selectList(null);
	}

	@Override
	public List<CheckinRecord> getCheckinRecordsEfficiently() {
		return baseMapper.selectCheckinRecords();
	}

	@Override
	public List<CheckinRecord> getCheckinRecordByAttendeesId(Long attendeesId) {
		// 找到這個與會者所有的checkin紀錄
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);
		return baseMapper.selectList(checkinRecordWrapper);
	}

	@Override
	public List<CheckinRecord> getCheckinRecordByAttendeesIds(Collection<Long> attendeesIds) {
		if (attendeesIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.in(CheckinRecord::getAttendeesId, attendeesIds);
		return baseMapper.selectList(checkinRecordWrapper);

	}

	@Override
	public IPage<CheckinRecord> getCheckinRecordPage(Page<CheckinRecord> page) {
		return baseMapper.selectPage(page, null);
	}

	@Override
	public Map<Long, List<CheckinRecord>> getCheckinMapByAttendeesList(Collection<Attendees> attendeesList) {
		// 1.提取與會者列表的ID
		Set<Long> attendeesIdSet = attendeesList.stream().map(Attendees::getAttendeesId).collect(Collectors.toSet());
		// 2.拿到符合與會者ID列表的 所有簽到退紀錄
		List<CheckinRecord> checkinRecords = this.getCheckinRecordByAttendeesIds(attendeesIdSet);
		// 3.如果簽到記錄為空,直接返回
		if (checkinRecords.isEmpty()) {
			return Collections.emptyMap();
		}
		// 4.根據 attendeesId 群組化
		return checkinRecords.stream().collect(Collectors.groupingBy(CheckinRecord::getAttendeesId));
	}

	@Override
	public Map<Long, Boolean> getCheckinStatusMap(Map<Long, List<CheckinRecord>> checkinMap) {
		// 1.預定義用來儲存與會者的簽到狀態
		Map<Long, Boolean> statusMap = new HashMap<>();

		// 2.透過Map.entrySet, 獲取key,value 的每次遍歷值
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

	@Override
	public CheckinRecordVO walkInRegistration(Long attendeesId) {
		// 1.幫現場註冊的與會者產生簽到記錄
		CheckinRecord checkinRecord = new CheckinRecord();
		checkinRecord.setAttendeesId(attendeesId);
		checkinRecord.setActionType(CheckinActionTypeEnum.CHECKIN.getValue());
		baseMapper.insert(checkinRecord);

		// 2.返回簽到時的顯示格式
		return checkinRecordConvert.entityToVO(checkinRecord);

	}

	@Override
	public CheckinRecord addCheckinRecord(AddCheckinRecordDTO addCheckinRecordDTO) {
		// 1.查詢指定 AttendeesId 最新的一筆
		CheckinRecord latestRecord = baseMapper.selectOne(new LambdaQueryWrapper<CheckinRecord>()
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
		baseMapper.insert(checkinRecord);

		// 6.準備返回的數據
		return checkinRecord;

	}

	@Override
	public void undoLastCheckin(Long attendeesId) {
		//查詢此與會者的最後一筆簽到/退資料
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId)
				.orderByDesc(CheckinRecord::getActionTime)
				.last("LIMIT 1");
		CheckinRecord checkinRecord = baseMapper.selectOne(checkinRecordWrapper);
		if (checkinRecord == null) {
			throw new CheckinRecordException("此與會者尚未簽到或簽退");
		}

		// 如果最後一筆資料為簽到,則刪除此筆簽到資料
		if (checkinRecord.getActionType().equals(CheckinActionTypeEnum.CHECKIN.getValue())) {
			baseMapper.deleteById(checkinRecord);
			return;
		}

		throw new CheckinRecordException("最後一筆資料不是簽到行為，無法撤銷");

	};

	@Override
	public void updateCheckinRecord(PutCheckinRecordDTO putCheckinRecordDTO) {
		CheckinRecord checkinRecord = checkinRecordConvert.putDTOToEntity(putCheckinRecordDTO);
		baseMapper.updateById(checkinRecord);
	}

	@Override
	public void deleteCheckinRecord(Long checkinRecordId) {
		baseMapper.deleteById(checkinRecordId);
	}

	@Override
	public void deleteCheckinRecordByAttendeesId(Long attendeesId) {
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);
		baseMapper.delete(checkinRecordWrapper);
	}

	@Override
	public void deleteCheckinRecordList(List<Long> checkinRecordIds) {
		for (Long checkinRecordId : checkinRecordIds) {
			this.deleteCheckinRecord(checkinRecordId);
		}
	}

	@Override
	public Integer getCountCheckedIn() {
		return baseMapper.countCheckedIn();
	}

	@Override
	public PresenceStatsBO getPresenceStats() {
		return baseMapper.selectPresenceStats();
	}

	@Override
	public CheckinInfoBO getLastCheckinRecordByAttendeesId(Long attendeesId) {
		// 先找到這個與會者所有的checkin紀錄
		LambdaQueryWrapper<CheckinRecord> checkinRecordWrapper = new LambdaQueryWrapper<>();
		checkinRecordWrapper.eq(CheckinRecord::getAttendeesId, attendeesId);
		List<CheckinRecord> checkinRecordList = baseMapper.selectList(checkinRecordWrapper);

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

}
