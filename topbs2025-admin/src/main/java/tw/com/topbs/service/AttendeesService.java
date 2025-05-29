package tw.com.topbs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.google.zxing.WriterException;

import jakarta.servlet.http.HttpServletResponse;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.WalkInRegistrationDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddAttendeesDTO;
import tw.com.topbs.pojo.VO.AttendeesStatsVO;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
public interface AttendeesService extends IService<Attendees> {

	AttendeesVO getAttendees(Long id);

	List<AttendeesVO> getAttendeesList();

	IPage<AttendeesVO> getAttendeesPage(Page<Attendees> page);

	Long addAfterPayment(AddAttendeesDTO addAttendees);

	void addAttendees(AddAttendeesDTO addAttendees);

	void deleteAttendees(Long attendeesId);

	void batchDeleteAttendees(List<Long> attendeesIds);

	/**
	 * 下載所有與會者列表
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * 
	 */
	void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException, IOException;

	/** 以下跟tag有關 */

	/**
	 * 根據 id 獲取與會者資訊 和 Tag標籤
	 * 
	 * @param id
	 * @return
	 */
	AttendeesTagVO getAttendeesTagVO(Long attendeesId);

	/**
	 * 獲取所有與會者資訊 和 Tag標籤(分頁)
	 * 
	 * @param pageInfo
	 * @return
	 */
	IPage<AttendeesTagVO> getAttendeesTagVOPage(Page<Attendees> pageInfo);

	/**
	 * 根據條件參數,獲取所有與會者資訊 和 Tag標籤(分頁)
	 * 
	 * @param pageInfo
	 * @param queryText
	 * @return
	 */
	IPage<AttendeesTagVO> getAttendeesTagVOPageByQuery(Page<Attendees> pageInfo, String queryText);

	/**
	 * 為與會者新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param memberId
	 */
	void assignTagToAttendees(List<Long> targetTagIdList, Long memberId);

	/**
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的Attendees
	 * 如果沒有傳任何tag則是寄給所有Attendees
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 * @throws IOException
	 * @throws WriterException
	 */
	void sendEmailToAttendeess(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	
	/**
	 * 查詢與會者的簽到的統計資料
	 * 
	 * @return
	 */
	AttendeesStatsVO getAttendeesStatsVO();

	/**
	 * 現場登記(包含註冊 - 簽到)
	 * 
	 * @param walkInRegistrationDTO
	 * @return
	 * @throws IOException
	 * @throws Exception
	 */
	CheckinRecordVO walkInRegistration(WalkInRegistrationDTO walkInRegistrationDTO) throws Exception, IOException;

	
}
