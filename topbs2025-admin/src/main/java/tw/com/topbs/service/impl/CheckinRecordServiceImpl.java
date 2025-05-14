package tw.com.topbs.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
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
import tw.com.topbs.manager.MemberManager;
import tw.com.topbs.mapper.CheckinRecordMapper;
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
public class CheckinRecordServiceImpl extends ServiceImpl<CheckinRecordMapper, CheckinRecord> implements CheckinRecordService {
	private final CheckinRecordConvert checkinRecordConvert;
	private final AttendeesConvert attendeesConvert;

	private final AttendeesManager attendeesManager;
	private final MemberManager memberManager;

	@Override
	public CheckinRecordVO getCheckinRecord(Long checkinRecordId) {

		// 1.查詢簽到/退紀錄
		CheckinRecord checkinRecord = baseMapper.selectById(checkinRecordId);

		// 2.查詢此簽到者的基本資訊
		AttendeesVO attendeesVO = attendeesManager.getAttendeesVOByAttendeesId(checkinRecord.getAttendeesId());

		// 3.實體類轉換成VO
		CheckinRecordVO checkinRecordVO = checkinRecordConvert.entityToVO(checkinRecord);

		// 4.vo中填入與會者VO對象
		checkinRecordVO.setAttendeesVO(attendeesVO);

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
	public CheckinRecordVO addCheckinRecord(AddCheckinRecordDTO addCheckinRecordDTO) {

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
		return this.getCheckinRecord(checkinRecord.getCheckinRecordId());

	}

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
		List<AttendeesVO> attendeesVOList = attendeesManager.getAttendeesVOByIds(attendeesIdSet);

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

	};
}
