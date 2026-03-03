package tw.com.topbs.manager;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import tw.com.topbs.config.RegistrationFeeConfig;
import tw.com.topbs.constants.OrderConstants;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.enums.OrderStatusEnum;
import tw.com.topbs.enums.RegistrationPhaseEnum;
import tw.com.topbs.exception.CheckinRecordException;
import tw.com.topbs.exception.MemberException;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.CheckinRecordService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.SettingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberManager {

	@Value("${project.email.reply-to}")
	private String EMAIL_REPLY_TO;

	@Value("${project.rate}")
	private Long RATE;

	@Value("${project.group-discount}")
	private Double GROUP_DISCOUNT;

	// 參加證明 Template Path
	private final String CERTIFICATE_TEMPLATE_PATH = "jasperTemplate/certificate.jasper";
	private final String CERTIFICATE_TEMPLATE_BG_PATH = "jasperTemplate/certificate.jpg";

	// Invoice Template Path
	private final String INVOICE_TEMPLATE_PATH = "jasperTemplate/conference_invoice.jasper";
	private final String INVOICE_SUBREPORT_TEMPLATE_PATH = "jasperTemplate/orderItems.jasper";
	private final String INVOICE_TEMPLATE_BG_PATH = "jasperTemplate/conference_invoice.jpg";

	private RegistrationFeeConfig registrationFeeConfig;
	private final MemberService memberService;
	private final OrdersService ordersService;
	private final AttendeesService attendeesService;
	private final CheckinRecordService checkinRecordService;
	private final SettingService settingService;

	/**
	 * 產生參加證明
	 * 
	 * @param response
	 * @param memberId
	 * @throws IOException
	 */
	public void generateCertificate(HttpServletResponse response, Long memberId) throws IOException {

		// 1.查詢會員是否是與會者的資格
		Attendees attendees = attendeesService.getAttendeesByMemberId(memberId);
		if (attendees == null) {
			throw new CheckinRecordException("會員未繳費並非與會者");
		}

		// 2.查詢與會者是否有簽到記錄，如果不是用我們的簽到系統,或者不需要那麼嚴格就註解掉
		long checkinRecordCount = checkinRecordService.getCheckinRecordCountByAttendeesId(attendees.getAttendeesId());
		if (checkinRecordCount < 1) {
			throw new CheckinRecordException("與會者沒有簽到記錄，不發參加證明");
		}

		// 3.引入certificate(參加證明)  Jasper文件(模板)
		Resource resource = new ClassPathResource(CERTIFICATE_TEMPLATE_PATH);
		InputStream mainInputStream = resource.getInputStream();
		// 4.引入certificate(參加證明) 背景圖片
		Resource bgResource = new ClassPathResource(CERTIFICATE_TEMPLATE_BG_PATH);
		InputStream bgInputStream = bgResource.getInputStream();

		// 5.透過response得到響應輸出流
		ServletOutputStream outputStream = response.getOutputStream();

		// 6.準備資料,製作參加證明 PDF
		try {

			// 6-1 初始化要給報表的Paramter Map對象
			Map<String, Object> parameters = new HashMap<>();

			// 6-2 拿到Member的資料
			Member member = memberService.getMember(memberId);

			// 6-3 準備證書上的姓名
			String firstName = StringUtils.trimToEmpty(member.getFirstName());
			String lastName = StringUtils.trimToEmpty(member.getLastName());

			// 只有在 first / last 非空時才加入，避免出現多餘空格
			String enName = Stream.of(firstName, lastName)
					.filter(StringUtils::isNotBlank)
					.collect(Collectors.joining(" "));
			String chinese = StringUtils.trimToEmpty(member.getChineseName());

			// 最後組合,先中文,後英文
			String finalName = Stream.of(chinese, enName)
					.filter(StringUtils::isNotBlank)
					.collect(Collectors.joining(" "));

			// 6-4 放入模板需要的資料
			parameters.put("finalName", finalName);
			parameters.put("bg", bgInputStream);

			/**
			 * 填充報表
			 * 
			 * 務必!!要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
			 * 第一個參數為: 文件輸入流 InputStream , 準確來說是 主報表 .jasper文件
			 * 第二個參數為: Map對象 向模板中輸入的參數 $P{} ,
			 * 通常是String、InputStream、List、Set這類的,SubReport常搭配List、Set使用
			 * 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據) , $F{}
			 * 第三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new
			 * JREmptyDataSource()來替代
			 * 
			 */
			JasperPrint print = JasperFillManager.fillReport(mainInputStream, parameters, new JREmptyDataSource());

			// 3.將JasperPrint以PDF形式輸出
			// 透過JasperExportManager工具類使用exportReportToPdfFile
			// 傳遞第一個參數JasperPrint對象
			// 傳遞第二個參數outputStream
			JasperExportManager.exportReportToPdfStream(print, outputStream);

		} catch (JRException e) {
			log.error(e.getMessage());
			e.printStackTrace();

		} finally {
			// 最終關閉這個響應輸出流 , 以及輸入流
			outputStream.close();
			mainInputStream.close();
			bgInputStream.close();
		}

	}

	/**
	 * 產生繳費證明
	 * 
	 * @param response
	 * @param memberId
	 * @throws IOException
	 */
	public void generateConferenceInvoice(HttpServletResponse response, Long memberId) throws IOException {

		// 1.判斷會員是否有繳註冊費(報名費)
		Orders order = ordersService.getRegistrationOrderByMemberId(memberId);
		if (order.getStatus().equals(OrderStatusEnum.UNPAID.getValue())) {
			throw new MemberException("會員未繳註冊費 , 不給予Invoice");
		}

		// 2.引入 Invoice(繳費證明) Jasper文件(模板) + 子報表模板
		Resource resource = new ClassPathResource(INVOICE_TEMPLATE_PATH);
		Resource subReportResource = new ClassPathResource(INVOICE_SUBREPORT_TEMPLATE_PATH);
		InputStream mainInputStream = resource.getInputStream();
		InputStream subReportInputStream = subReportResource.getInputStream();

		// 3.引入 Invoice(繳費證明) 背景圖片
		Resource bgResource = new ClassPathResource(INVOICE_TEMPLATE_BG_PATH);
		InputStream bgInputStream = bgResource.getInputStream();

		// 4.透過response得到響應輸出流,不做設置直接響應
		ServletOutputStream outputStream = response.getOutputStream();

		// 5.準備資料,製作參加證明 PDF
		try {
			// 5-1 初始化要給報表的Paramter Map對象
			Map<String, Object> parameters = new HashMap<>();

			// 5-2拿到活動日期
			Setting setting = settingService.getSetting();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d ,yyyy", Locale.ENGLISH);
			String eventDate = setting.getEventStartDate().format(formatter);

			// 5-3業務上來說,有繳費一定是與會者,這邊就不多判斷
			Member member = memberService.getMember(memberId);

			// 5-4準備姓名,去掉空格
			String firstName = StringUtils.trimToEmpty(member.getFirstName());
			String lastName = StringUtils.trimToEmpty(member.getLastName());
			String enName = Stream.of(firstName, lastName)
					.filter(StringUtils::isNotBlank)
					.collect(Collectors.joining(" "));

			// 5-5與會者資料
			Attendees attendees = attendeesService.getAttendeesByMemberId(memberId);

			// 先拿到訂單 台幣價格
			BigDecimal twdAmount = order.getTotalAmount();

			// 5-6 如果itemsSummary為Group Registration Fee , 代表是團體報名 , 那台幣金額要重算
			if (order.getItemsSummary().equals(OrderConstants.GROUP_ITEMS_SUMMARY_REGISTRATION)) {

				// 1.拿到配置設定,知道處於哪個註冊階段
				RegistrationPhaseEnum registrationPhaseEnum = settingService
						.getRegistrationPhaseEnum(member.getCreateDate());

				// 2.拿到身分
				MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(member.getCategory());

				// 3.透過階段、國籍、身分，得到金額
				BigDecimal membershipFee = registrationFeeConfig.getFee(registrationPhaseEnum.getValue(),
						member.getCountry(), memberCategoryEnum.getConfigKey());

				// 4.金額還要再打團體報名的優惠折扣
				twdAmount = membershipFee.multiply(BigDecimal.valueOf(GROUP_DISCOUNT));
			}

			// 5-7訂單資料,拿到美金折算匯率,計算並保留兩位小數,四捨五入規則,最後固定小數點後兩位
			BigDecimal rate = new BigDecimal(RATE);
			BigDecimal usdAmount = twdAmount.divide(rate, 2, RoundingMode.HALF_UP);

			//目前只有繳註冊費的功能,所以只有一筆訂單,直接修改成美元金額就好
			order.setTotalAmount(usdAmount);

			parameters.put("finalName", enName);
			parameters.put("eventDate", eventDate);
			parameters.put("sequenceNo", String.format("%03d", attendees.getSequenceNo()));
			parameters.put("totalAmount", usdAmount);
			parameters.put("contactEmail", EMAIL_REPLY_TO);
			parameters.put("bg", bgInputStream);
			parameters.put("subReport", subReportInputStream);

			parameters.put("orderItems", Arrays.asList(order));

			/**
			 * 填充報表
			 * 
			 * 務必!!要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
			 * 第一個參數為: 文件輸入流 InputStream , 準確來說是 主報表 .jasper文件
			 * 第二個參數為: Map對象 向模板中輸入的參數 $P{},
			 * 通常是String、InputStream、List、Set這類的,SubReport常搭配List、Set使用
			 * 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據) , $F{}
			 * 第三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new
			 * JREmptyDataSource()來替代
			 * 
			 */
			JasperPrint print = JasperFillManager.fillReport(mainInputStream, parameters, new JREmptyDataSource());

			// 3.將JasperPrint以PDF形式輸出
			// 透過JasperExportManager工具類使用exportReportToPdfFile
			// 傳遞第一個參數JasperPrint對象
			// 傳遞第二個參數outputStream
			JasperExportManager.exportReportToPdfStream(print, outputStream);
		} catch (JRException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		} finally {
			// 最終關閉這個響應輸出流
			outputStream.close();
			mainInputStream.close();
			subReportInputStream.close();
			bgInputStream.close();
		}
	}

	/**
	 * 刪除單個會員<br>
	 * 包含其與會者身分 和 簽到退紀錄
	 * 
	 * @param memberId
	 */
	@Transactional
	public void deleteMember(Long memberId) {
		// 1.刪除會員的與會者身分
		Attendees attendees = attendeesService.deleteAttendeesByMemberId(memberId);

		// 2.如果attendees不為null，刪除他的簽到/退紀錄
		if (attendees != null) {
			checkinRecordService.deleteCheckinRecordByAttendeesId(attendees.getAttendeesId());
		}

		// 3.最後刪除自身
		memberService.deleteMember(memberId);

	}

	/**
	 * 批量刪除單個會員<br>
	 * 包含其與會者身分 和 簽到退紀錄
	 * 
	 * @param memberId
	 */
	@Transactional
	public void deleteMemberList(Collection<Long> memberIds) {
		for (Long memberId : memberIds) {
			this.deleteMember(memberId);
		}
	}

}
