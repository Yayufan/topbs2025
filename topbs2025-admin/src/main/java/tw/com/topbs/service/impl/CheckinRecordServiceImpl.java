package tw.com.topbs.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.convert.CheckinRecordConvert;
import tw.com.topbs.enums.CheckinActionTypeEnum;
import tw.com.topbs.exception.CheckinRecordException;
import tw.com.topbs.manager.AttendeesManager;
import tw.com.topbs.manager.CheckinRecordManager;
import tw.com.topbs.manager.MemberManager;
import tw.com.topbs.mapper.CheckinRecordMapper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.excelPojo.AttendeesExcel;
import tw.com.topbs.pojo.excelPojo.CheckinRecordExcel;
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
	private final AttendeesConvert attendeesConvert;

	private final AttendeesManager attendeesManager;
	private final MemberManager memberManager;
	private final CheckinRecordManager checkinRecordManager;

	@Override
	public CheckinRecordVO getCheckinRecord(Long checkinRecordId) {

		// 1.查詢簽到/退紀錄
		CheckinRecord checkinRecord = baseMapper.selectById(checkinRecordId);

		// 2.查詢此簽到者的基本資訊, 2025/9/24 重構臨時註解
		//		AttendeesVO attendeesVO = attendeesManager.getAttendeesVOByAttendeesId(checkinRecord.getAttendeesId());

		// 3.實體類轉換成VO
		CheckinRecordVO checkinRecordVO = checkinRecordConvert.entityToVO(checkinRecord);

		// 4.vo中填入與會者VO對象  2025/9/24 重構臨時註解
		//		checkinRecordVO.setAttendeesVO(attendeesVO);

		return checkinRecordVO;
	}

	@Override
	public List<CheckinRecordVO> getCheckinRecordList() {

		// 1.查詢所有簽到/退紀錄
		List<CheckinRecord> checkinRecordList = baseMapper.selectList(null);

		// 2.使用私有方法獲取CheckinRecordVOList
		List<CheckinRecordVO> checkinRecordVOList = this.convertToCheckinRecordVOList(checkinRecordList);

		return checkinRecordVOList;
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
	public IPage<CheckinRecordVO> getCheckinRecordPage(Page<CheckinRecord> page) {

		// 1.先獲取Page的資訊
		Page<CheckinRecord> checkinRecordPage = baseMapper.selectPage(page, null);

		// 2.使用私有方法獲取CheckinRecordVOList
		List<CheckinRecordVO> checkinRecordVOList = this.convertToCheckinRecordVOList(checkinRecordPage.getRecords());

		// 3.封裝成VOpage
		Page<CheckinRecordVO> checkinRecordVOPage = new Page<>(checkinRecordPage.getCurrent(),
				checkinRecordPage.getSize(), checkinRecordPage.getTotal());
		checkinRecordVOPage.setRecords(checkinRecordVOList);

		return checkinRecordVOPage;
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
	public CheckinRecordVO addCheckinRecord(AddCheckinRecordDTO addCheckinRecordDTO) {

		// 1.新增簽到記錄
		CheckinRecord checkinRecord = checkinRecordManager.addCheckinRecord(addCheckinRecordDTO);

		// 2.準備返回的數據
		return this.getCheckinRecord(checkinRecord.getCheckinRecordId());

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

	private List<CheckinRecordVO> convertToCheckinRecordVOList(List<CheckinRecord> checkinRecordList) {

		// 1.獲取與會者的ID(去重)
		Set<Long> attendeesIdSet = checkinRecordList.stream()
				.map(CheckinRecord::getAttendeesId)
				.collect(Collectors.toSet());

		// 2.透過去重的與會者ID拿到資料
		List<AttendeesVO> attendeesVOList = attendeesManager.getAttendeesVOByAttendeesIds(attendeesIdSet);

		// 3.做成資料映射attendeesID 對應 AttendeesVO
		Map<Long, AttendeesVO> AttendeesVOMap = attendeesVOList.stream()
				.collect(Collectors.toMap(AttendeesVO::getAttendeesId, Function.identity()));

		// 4.checkinRecordList stream轉換後映射組裝成VO對象
		List<CheckinRecordVO> checkinRecordVOList = checkinRecordList.stream().map(checkinRecord -> {
			CheckinRecordVO vo = checkinRecordConvert.entityToVO(checkinRecord);
			vo.setAttendeesVO(AttendeesVOMap.get(checkinRecord.getAttendeesId()));
			return vo;
		}).collect(Collectors.toList());

		return checkinRecordVOList;
	}

	@Override
	public void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException, IOException {

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("簽到退紀錄名單", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 查詢所有簽到/退資料
		List<CheckinRecord> checkinRecordList = baseMapper.selectCheckinRecords();

		// 查詢所有會員，用來填充與會者的基本資訊
		List<Member> memberList = memberManager.getAllMembersEfficiently();

		// 轉成一對一 Map，key為 memberId, value為訂單本身
		Map<Long, Member> memberMap = memberList.stream()
				.collect(Collectors.toMap(Member::getMemberId, Function.identity()));

		// 獲取所有與會者 和 對應的映射關係
		List<Attendees> attendeesList = attendeesManager.getAttendeesList();

		Map<Long, Attendees> attendeesMap = attendeesList.stream()
				.collect(Collectors.toMap(Attendees::getAttendeesId, Function.identity()));

		// 資料轉換成Excel
		List<CheckinRecordExcel> excelData = checkinRecordList.stream().map(checkinRecord -> {
			// 透過attendeesId先拿到attendeesVO
			AttendeesVO attendeesVO = attendeesConvert.entityToVO(attendeesMap.get(checkinRecord.getAttendeesId()));
			// 再透過 memberId放入Member
			attendeesVO.setMember(memberMap.get(attendeesVO.getMemberId()));
			// 獲取到AttendeesExcel 再轉換成 CheckinRecordExcel
			AttendeesExcel attendeesExcel = attendeesConvert.voToExcel(attendeesVO);
			CheckinRecordExcel checkinRecordExcel = checkinRecordConvert
					.attendeesExcelToCheckinRecordExcel(attendeesExcel);

			//最後再補上缺失的屬性
			checkinRecordExcel.setActionTime(checkinRecord.getActionTime());
			checkinRecordExcel.setActionType(CheckinActionTypeEnum.fromValue(checkinRecord.getActionType()).getLabel());
			checkinRecordExcel.setLocation(checkinRecord.getLocation());
			checkinRecordExcel.setCheckinRecordId(checkinRecord.getCheckinRecordId().toString());
			checkinRecordExcel.setRemark(checkinRecord.getRemark());
			return checkinRecordExcel;

		}).collect(Collectors.toList());

		EasyExcel.write(response.getOutputStream(), CheckinRecordExcel.class).sheet("簽到退紀錄列表").doWrite(excelData);

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
