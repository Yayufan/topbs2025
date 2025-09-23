package tw.com.topbs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
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
	 * 查詢所有簽到/退 紀錄(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<CheckinRecordVO> getCheckinRecordPage(Page<CheckinRecord> page);

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

}
