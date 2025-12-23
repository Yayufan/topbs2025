package tw.com.topbs.manager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.zxing.WriterException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.handler.AttendeesVOHandler;
import tw.com.topbs.helper.TagAssignmentHelper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.WalkInRegistrationDTO;
import tw.com.topbs.pojo.VO.AttendeesStatsVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.VO.ImportResultVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.excelPojo.AttendeesExcel;
import tw.com.topbs.pojo.excelPojo.AttendeesUpdateExcel;
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

/**
 * AttendeesProfileManager，處理與會者個人資料相關的管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendeesProfileManager {

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	private final TagAssignmentHelper tagAssignmentHelper;
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

		// 3.獲取當下Member群體的Index,進行會員標籤分組
		tagAssignmentHelper.assignTag(member.getMemberId(), memberService::getMemberGroupIndex,
				tagService::getOrCreateMemberGroupTag, memberTagService::addMemberTag);

		// 4.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 5.獲取當下與會者群體的Index,進行與會者標籤分組
		tagAssignmentHelper.assignTag(attendees.getAttendeesId(), attendeesService::getAttendeesGroupIndex,
				tagService::getOrCreateAttendeesGroupTag, attendeesTagService::addAttendeesTag);

		// 6.獲取AttendeesVO
		AttendeesVO attendeesVO = this.getAttendeesVO(attendees.getAttendeesId());

		// 7.產生簽到記錄並組裝返回VO
		CheckinRecordVO checkinRecordVO = checkinRecordService.walkInRegistration(attendees.getAttendeesId());
		checkinRecordVO.setAttendeesVO(attendeesVO);

		// 8.產生現場註冊的信件,包含QRcode信息
		EmailBodyContent walkInRegistrationContent = notificationService
				.generateWalkInRegistrationContent(attendees.getAttendeesId(), BANNER_PHOTO_URL);

		// 9.透過異步工作去寄送郵件，因為使用了事務，在事務提交後才執行寄信的異步操作，安全做法
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				asyncService.sendCommonEmail(member.getEmail(), "【" + PROJECT_NAME + " 報到確認】現場報到用 QR Code 及活動資訊",
						walkInRegistrationContent.getHtmlContent(), walkInRegistrationContent.getPlainTextContent());
			}
		});

		// 10.返回簽到顯示格式
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
				log.error("QRcode產生失敗");
				e.printStackTrace();
			}

			return attendeesExcel;

		}).collect(Collectors.toList());

		EasyExcel.write(response.getOutputStream(), AttendeesExcel.class).sheet("與會者列表").doWrite(excelData);

	}

	/**
	 * 匯入Excel 批量更新與會者 收據統編號碼
	 * 
	 * @param file
	 * @throws IOException
	 */
	public ImportResultVO importExcelUpdate(MultipartFile file) throws IOException {

		// 統一返回結果的對象
		ImportResultVO result = new ImportResultVO();

		/**
		 * Excel 搭配監聽器讀取,避免一次讀取避免OOM
		 * 
		 * @param 檔案的inputStream
		 * @param 對應的Class
		 * @param 監聽器內部方法
		 * 
		 */
		EasyExcel.read(file.getInputStream(), AttendeesExcel.class, new ReadListener<AttendeesExcel>() {

			// 批次數量
			private static final int BATCH_COUNT = 500;
			// 更新暫存列表
			private List<Attendees> cachedDataList = new ArrayList<>();

			// 每讀取到一行就執行invoke函數
			@Override
			public void invoke(AttendeesExcel row, AnalysisContext context) {

				// Excel 行號從1開始
				int rowIndex = context.readRowHolder().getRowIndex() + 1;
				result.setTotalCount(result.getTotalCount() + 1);

				// 初始化attendeesId用來記錄,如果從excel中成功讀取就會友值
				String attendeesId = "unknown";
				if (row != null && row.getAttendeesId() != null) {
					attendeesId = row.getAttendeesId().toString();
				}

				try {

					//轉換資料
					AttendeesUpdateExcel excelToUpdatePojo = attendeesConvert.excelToUpdatePojo(row);
					Attendees attendee = attendeesConvert.updatePojoToEntity(excelToUpdatePojo);
					cachedDataList.add(attendee);

					if (cachedDataList.size() >= BATCH_COUNT) {
						attendeesService.saveOrUpdateBatch(cachedDataList);
						result.setSuccessCount(result.getSuccessCount() + cachedDataList.size());
						cachedDataList.clear();
					}
				} catch (Exception e) {
					// 捕獲單行錯誤，不影響整個批次
					String messageWithId = String.format("主鍵ID=%s, %s", attendeesId, e.getMessage());
					result.getFailList().add(new ImportResultVO.FailDetail(rowIndex, messageWithId));

					log.error("第 {} 行資料處理失敗: {}", rowIndex, messageWithId, e);
				}

			}

			// 當 Excel 全部讀完後，會呼叫 doAfterAllAnalysed()。
			@Override
			public void doAfterAllAnalysed(AnalysisContext context) {
				// 如果緩存內還有檔案,則最後再更新一次
				if (!cachedDataList.isEmpty()) {
					try {
						attendeesService.saveOrUpdateBatch(cachedDataList);
						result.setSuccessCount(result.getSuccessCount() + cachedDataList.size());
					} catch (Exception e) {
						// 批次失敗直接記錄所有行為失敗
						int rowStart = result.getTotalCount() - cachedDataList.size() + 1;
						for (int i = 0; i < cachedDataList.size(); i++) {
							Attendees attendee = cachedDataList.get(i);
							int rowNumber = rowStart + i;
							String attendeesIdBatch = attendee.getAttendeesId() != null
									? attendee.getAttendeesId().toString()
									: "unknown";
							String messageBatch = String.format("主鍵ID=%s, %s", attendeesIdBatch, e.getMessage());

							result.getFailList().add(new ImportResultVO.FailDetail(rowNumber, messageBatch));

							log.error("第 {} 行批次更新失敗: {}", rowNumber, messageBatch, e);
						}
					}

				}
				// 從錯誤清單中拿到總數
				result.setFailCount(result.getFailList().size());
			}
		}).sheet().doRead();

		return result;

	}

}
