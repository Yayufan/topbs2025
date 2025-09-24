package tw.com.topbs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.CheckinRecord;

/**
 * <p>
 * 簽到退紀錄 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
public interface CheckinRecordService extends IService<CheckinRecord> {

	/**
	 * 根據 checkinRecordId 獲取簽到/退紀錄
	 * 
	 * @param checkinRecordId
	 * @return
	 */
	CheckinRecordVO getCheckinRecord(Long checkinRecordId);

	/**
	 * 查詢所有簽到/退 紀錄
	 * 
	 * @return
	 */
	List<CheckinRecordVO> getCheckinRecordList();
	
	
	
	/**
	 * 根據 attendeesId 找到與會者所有簽到/退紀錄
	 * 
	 * @param attendeesId
	 * @return
	 */
	List<CheckinRecord> getCheckinRecordByAttendeesId(Long attendeesId);
	
	/**
	 * 根據 attendeesIds 找到範圍內與會者 所有簽到/退紀錄
	 * 
	 * @param attendeesIds
	 * @return
	 */
	List<CheckinRecord> getCheckinRecordByAttendeesIds(Collection<Long> attendeesIds);

	/**
	 * 查詢所有簽到/退 紀錄(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<CheckinRecordVO> getCheckinRecordPage(Page<CheckinRecord> page);

	
	/**
	 * 根據 attendeesList 創建與會者ID 和 簽到記錄的映射
	 * 
	 * @param attendeesList
	 * @return attendeesId 為key , List<CheckinRecord>為value的 Map對象
	 */
	public Map<Long, List<CheckinRecord>> getCheckinMapByAttendeesList(Collection<Attendees> attendeesList) ;
	
	/**
	 * 透過 與會者ID 和 簽到記錄的映射，再創建一個與會者與最後簽到狀態的映射
	 * 
	 * @param checkinMap
	 * @return
	 */
	public Map<Long, Boolean> getCheckinStatusMap(Map<Long, List<CheckinRecord>> checkinMap);
	
	/**
	 * 現場註冊產生的簽到記錄
	 * 
	 * @param attendeesId
	 * @return
	 */
	CheckinRecordVO walkInRegistration(Long attendeesId);
	
	/**
	 * 新增簽到/退紀錄
	 * 
	 * @param addCheckinRecordDTO
	 */
	CheckinRecordVO addCheckinRecord(AddCheckinRecordDTO addCheckinRecordDTO);

	/**
	 * 根據與會者ID,撤銷最後一筆簽到記錄
	 * 
	 * @param attendeesId
	 */
	void undoLastCheckin(Long attendeesId);

	/**
	 * 修改簽到/退紀錄
	 * 
	 * @param putCheckinRecordDTO
	 */
	void updateCheckinRecord(PutCheckinRecordDTO putCheckinRecordDTO);

	/**
	 * 刪除簽到/退紀錄
	 * 
	 * @param checkinRecordId
	 */
	void deleteCheckinRecord(Long checkinRecordId);

	/**
	 * 根據與會者ID,刪除他的簽到/退紀錄
	 * 
	 * @param attendeesId
	 */
	void deleteCheckinRecordByAttendeesId(Long attendeesId);

	/**
	 * 批量刪除簽到/退紀錄
	 * 
	 * @param checkinRecordIds
	 */
	void deleteCheckinRecordList(List<Long> checkinRecordIds);

	/**
	 * 下載所有簽到/退紀錄列表
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * 
	 */
	void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException, IOException;

	/**
	 * 查詢簽到的總人數
	 * 
	 * @return
	 */
	Integer getCountCheckedIn();

	/**
	 * 獲取 尚在現場、已離場 人數
	 * 
	 * @return
	 */
	public PresenceStatsBO getPresenceStats();
	
	/**
	 * 根據attendeesId , 找到這位與會者簡易的簽到退紀錄
	 * <p>
	 * (已最早的簽到紀錄 和 最晚的簽退紀錄組成)
	 * 
	 * @param attendeesId
	 * @return
	 */
	public CheckinInfoBO getLastCheckinRecordByAttendeesId(Long attendeesId);

}
