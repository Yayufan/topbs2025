package tw.com.topbs.manager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.util.Log;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.zxing.WriterException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.handler.AttendeesVOHandler;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.WalkInRegistrationDTO;
import tw.com.topbs.pojo.VO.AttendeesStatsVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.pojo.excelPojo.AttendeesExcel;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.CheckinRecordService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.NotificationService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.TagService;
import tw.com.topbs.utils.QrcodeUtil;

@Component
@RequiredArgsConstructor
public class AttendeesProfileManager {

	private String BANNER_PHOTO_URL = "https://iopbs2025.org.tw/_nuxt/banner.CL2lyu9P.png";
	private final int groupSize = 200;

	private final MemberService memberService;
	private final MemberTagService memberTagService;
	private final AttendeesService attendeesService;
	private final AttendeesTagService attendeesTagService;
	private final AttendeesConvert attendeesConvert;
	private final OrdersService ordersService;
	private final CheckinRecordService checkinRecordService;
	private final TagService tagService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	private final AttendeesVOHandler attendeesVOHandler;

	/**
	 * 根據 attendeesId 獲取 與會者完整資訊
	 * 
	 * @param attendeesId
	 * @return
	 */
	public AttendeesVO getAttendeesVO(Long attendeesId) {
		return attendeesVOHandler.getAttendeesVO(attendeesId);
	}

	/**
	 * 返回所有attendeesVO對象
	 * 
	 * @return
	 */
	public List<AttendeesVO> getAttendeesVOList() {
		// 1.獲取所有與會者資料
		List<Attendees> attendeesList = attendeesService.getAttendeesList();

		// 2.轉換並返回VOList
		return attendeesVOHandler.getAttendeesVOsByAttendeesList(attendeesList);

	}

	/**
	 * 返回所有attendeesVO 分頁對象
	 * 
	 * @param page
	 * @return
	 */
	public IPage<AttendeesVO> getAttendeesVOPage(Page<Attendees> page) {
		// 1.獲取與會者分頁對象
		IPage<Attendees> attendeesPage = attendeesService.getAttendeesPage(page);
		// 2.轉換並返回VOList
		List<AttendeesVO> attendeesVOList = attendeesVOHandler
				.getAttendeesVOsByAttendeesList(attendeesPage.getRecords());
		// 3.封裝成VOpage
		Page<AttendeesVO> attendeesVOPage = new Page<>(attendeesPage.getCurrent(), attendeesPage.getSize(),
				attendeesPage.getTotal());
		attendeesVOPage.setRecords(attendeesVOList);

		return attendeesVOPage;
	}

	/**
	 * 返回當前與會者簽/退的統計資料
	 * 
	 * @return
	 */
	public AttendeesStatsVO getAttendeesStatsVO() {
		AttendeesStatsVO attendeesStatsVO = new AttendeesStatsVO();
		//1.查詢 應到 人數
		Integer countTotalShouldAttend = attendeesService.countTotalShouldAttend();
		attendeesStatsVO.setTotalShouldAttend(countTotalShouldAttend);

		//2.查詢 已簽到 人數
		Integer countCheckedIn = checkinRecordService.getCountCheckedIn();
		attendeesStatsVO.setTotalCheckedIn(countCheckedIn);
		//未簽到人數
		attendeesStatsVO.setTotalNotArrived(countTotalShouldAttend - countCheckedIn);

		//3.查詢 尚在現場、已離場 人數
		PresenceStatsBO presenceStatsBO = checkinRecordService.getPresenceStats();
		attendeesStatsVO.setTotalOnSite(presenceStatsBO.getTotalOnsite());
		attendeesStatsVO.setTotalLeft(presenceStatsBO.getTotalLeft());

		return attendeesStatsVO;
	}

	/**
	 * 現場註冊報名
	 * 
	 * @param walkInRegistrationDTO
	 * @return
	 */
	@Transactional
	public CheckinRecordVO walkInRegistration(WalkInRegistrationDTO walkInRegistrationDTO) {
		// 1.創建Member對象，新增進member table
		Member member = memberService.addMemberOnSite(walkInRegistrationDTO);

		// 2.創建已繳費訂單-預設他會在現場繳費完成
		ordersService.createFreeRegistrationOrder(member);

		// 3.獲取當下Member群體的Index,用於後續標籤分組
		int memberGroupIndex = memberService.getMemberGroupIndex(groupSize);

		// 4.會員標籤分組，拿到 Tag（不存在則新增Tag），關聯 Member 與 Tag
		Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 5.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 6.獲取當下 Attendees 群體的Index,用於後續標籤分組
		int attendeesGroupIndex = attendeesService.getAttendeesGroupIndex(groupSize);

		// 7.與會者標籤分組，拿到 Tag（不存在則新增Tag），關聯 Attendees 與 Tag
		Tag attendeesGroupTag = tagService.getOrCreateAttendeesGroupTag(attendeesGroupIndex);
		attendeesTagService.addAttendeesTag(attendees.getAttendeesId(), attendeesGroupTag.getTagId());

		// 8.獲取AttendeesVO
		AttendeesVO attendeesVO = this.getAttendeesVO(attendees.getAttendeesId());

		// 9.產生簽到記錄並組裝返回VO
		CheckinRecordVO checkinRecordVO = checkinRecordService.walkInRegistration(attendees.getAttendeesId());
		checkinRecordVO.setAttendeesVO(attendeesVO);

		// 10.產生現場註冊的信件,包含QRcode信息
		EmailBodyContent walkInRegistrationContent = notificationService
				.generateWalkInRegistrationContent(attendees.getAttendeesId(), BANNER_PHOTO_URL);

		// 11.透過異步工作去寄送郵件，因為使用了事務，在事務提交後才執行寄信的異步操作，安全做法
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				asyncService.sendCommonEmail(member.getEmail(), "【TICBCS 2025 報到確認】現場報到用 QR Code 及活動資訊",
						walkInRegistrationContent.getHtmlContent(), walkInRegistrationContent.getPlainTextContent());
			}
		});

		// 12.返回簽到顯示格式
		return checkinRecordVO;
	}

	/**
	 * 刪除與會者 及 其簽到/退紀錄
	 * 
	 * @param attendeesId
	 */
	public void deleteAttendees(Long attendeesId) {
		// 1.刪除與會者的簽到/退紀錄
		checkinRecordService.deleteCheckinRecordByAttendeesId(attendeesId);

		// 2.刪除與會者
		attendeesService.deleteAttendees(attendeesId);

	}

	/**
	 * 批量刪除與會者 及 其簽到/退紀錄
	 * 
	 * @param attendeesIds
	 */
	public void batchDeleteAttendees(List<Long> attendeesIds) {
		for (Long attendeesId : attendeesIds) {
			this.deleteAttendees(attendeesId);
		}

	}

	/**
	 * 下載與會者Excel
	 * 
	 * @param response
	 * @throws IOException
	 */
	public void downloadExcel(HttpServletResponse response) throws IOException {

		// 1.基礎設定
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("與會者名單", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 2.獲取所有會員的映射對象
		Map<Long, Member> memberMap = memberService.getMemberMap();

		// 3.高效獲取所有attendees
		List<Attendees> attendeesList = attendeesService.getAttendeesEfficiently();

		// 4.資料轉換成Excel
		List<AttendeesExcel> excelData = attendeesList.stream().map(attendees -> {

			// 4-1放入Member轉換成VO對象
			AttendeesVO attendeesVO = attendeesConvert.entityToVO(attendees);
			attendeesVO.setMember(memberMap.get(attendees.getMemberId()));
			AttendeesExcel attendeesExcel = attendeesConvert.voToExcel(attendeesVO);

			// 4-2 獲取與會者的簡易簽到記錄
			CheckinInfoBO checkinInfoBO = checkinRecordService
					.getLastCheckinRecordByAttendeesId(attendees.getAttendeesId());
			attendeesExcel.setFirstCheckinTime(checkinInfoBO.getCheckinTime());
			attendeesExcel.setLastCheckoutTime(checkinInfoBO.getCheckoutTime());

			// 4-3匯出專屬簽到/退 QRcode
			try {
				attendeesExcel.setQRcodeImage(
						QrcodeUtil.generateBase64QRCode(attendeesVO.getAttendeesId().toString(), 200, 200));
			} catch (WriterException | IOException e) {
				Log.error("QRcode產生失敗");
				e.printStackTrace();
			}

			return attendeesExcel;

		}).collect(Collectors.toList());

		EasyExcel.write(response.getOutputStream(), AttendeesExcel.class).sheet("與會者列表").doWrite(excelData);

	}

}
