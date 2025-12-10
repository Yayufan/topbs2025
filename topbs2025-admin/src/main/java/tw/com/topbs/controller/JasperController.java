package tw.com.topbs.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;

@RestController
@RequiredArgsConstructor
@RequestMapping("/jasper")
public class JasperController {

	private final MemberMapper memberMapper;

	// 簡單的PDF
	@GetMapping("testJasper")
	public void testPdf(HttpServletResponse response) throws FileNotFoundException, IOException {

		// 1.引入Jasper文件 , 獲得InputStream
		Resource resource = new ClassPathResource("jasperTemplate/Blank_A4.jasper");
		InputStream mainInputStream = resource.getInputStream();

		// 2.透過response得到響應輸出流
		ServletOutputStream outputStream = response.getOutputStream();

		// 3.準備數據,向Jasper文件填充數據
		try {

			/**
			 * 3-1填充報表
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
			JasperPrint print = JasperFillManager.fillReport(mainInputStream, new HashMap<>(), new JREmptyDataSource());

			// 3-2.將JasperPrint以PDF形式輸出
			// 透過JasperExportManager工具類使用exportReportToPdfFile
			// 傳遞第一個參數JasperPrint對象
			// 傳遞第二個參數outputStream
			JasperExportManager.exportReportToPdfStream(print, outputStream);

		} catch (JRException e) {
			e.printStackTrace();
		} finally {
			// 最終關閉這個響應輸出流
			outputStream.close();
			mainInputStream.close();
		}

	}

	// 透過Params傳遞參數,填入jasper文件中，測試時不能使用 knife4j 或者 swagger
	@GetMapping("testJasper02")
	public void testPdf02(HttpServletResponse response) throws FileNotFoundException, IOException {

		// 1.引入Jasper文件 , 並獲得 InputStream
		Resource resource = new ClassPathResource("jasperTemplate/Blank_A4.jasper");
		InputStream mainInputStream = resource.getInputStream();
		// 2.引入背景圖片 , 並獲得 InputStream
		Resource bgResource = new ClassPathResource("jasperTemplate/background.jpg");
		InputStream bgInputStream = bgResource.getInputStream();

		// 透過response得到響應輸出流
		ServletOutputStream outputStream = response.getOutputStream();

		// 實際完成要靠從minio獲取圖片,但也是網址,所以這是最接近的版本
		List<String> imageUrls = List.of("https://iopbs2025.org.tw/_nuxt/10_Stryker.B-YSotAr.png",
				"https://iopbs2025.org.tw/_nuxt/11_Sumtage.BMKSE1an.png",
				"https://iopbs2025.org.tw/_nuxt/13_Amgen.BocFfSFi.png",
				"https://iopbs2025.org.tw/_nuxt/14_BW_LOGO.CJiSyvMf.png",
				"https://iopbs2025.org.tw/_nuxt/1_Cell_Trion._pmPMe7y.png",
				"https://iopbs2025.org.tw/_nuxt/2_Lotus.EBzwG3-V.png",
				"https://iopbs2025.org.tw/_nuxt/6_Eisai.DC94uV_T.png",
				"https://iopbs2025.org.tw/_nuxt/4_Gentek.DmKiuhev.png");

		// 2.準備數據,向Jasper文件填充數據
		try {

			// 2-1 初始化paramter參數
			Map<String, Object> parameters = new HashMap<>();

			parameters.put("userName", "孫悟空");
			parameters.put("phone", "0985225586");
			parameters.put("company", "ZF");
			parameters.put("department", "IT部門");
			parameters.put("logoImg", "https://iopbs2025.org.tw/_nuxt/2_Pfizer.DYniZ15y.png");
			// 設定背景圖片
			parameters.put("bg", bgInputStream);

			// 傳入所有可能的圖片 URL (最多8張)
			for (int i = 0; i < 8; i++) {
				String imageUrl = i < imageUrls.size() ? imageUrls.get(i) : null;
				String key = String.format("img%02d", i + 1);
				parameters.put(key, imageUrl);
			}

			parameters.put("imgCount", imageUrls.size());

			/**
			 * 2-2填充報表
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

			// 2-3.將JasperPrint以PDF形式輸出
			// 透過JasperExportManager工具類使用exportReportToPdfFile
			// 傳遞第一個參數JasperPrint對象
			// 傳遞第二個參數outputStream
			JasperExportManager.exportReportToPdfStream(print, outputStream);

		} catch (JRException e) {
			e.printStackTrace();
		} finally {
			// 最終關閉這個響應輸出流
			outputStream.close();
			mainInputStream.close();
		}

	}
	
	// 透過Params傳遞參數,填入jasper文件中，測試時不能使用 knife4j 或者 swagger
	@GetMapping("AZ-demo")
	public void AZDemo(HttpServletResponse response) throws FileNotFoundException, IOException {

		// 1.引入Jasper文件 , 並獲得 InputStream
		Resource resource = new ClassPathResource("jasperTemplate/AZ_demo.jasper");
		InputStream mainInputStream = resource.getInputStream();
		// 2.引入背景圖片 , 並獲得 InputStream
		Resource bgResource = new ClassPathResource("jasperTemplate/background.jpg");
		InputStream bgInputStream = bgResource.getInputStream();

		// 透過response得到響應輸出流
		ServletOutputStream outputStream = response.getOutputStream();

		
		List<InputStream> isList = List.of(
				new ClassPathResource("jasperTemplate/02_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/03_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/04_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/05_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/06_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/07_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/08_thumb.png").getInputStream(),
				new ClassPathResource("jasperTemplate/01_thumb.png").getInputStream()
				);

		// 2.準備數據,向Jasper文件填充數據
		try {

			// 2-1 初始化paramter參數
			Map<String, Object> parameters = new HashMap<>();

			parameters.put("userName", "孫悟空");
			parameters.put("phone", "0985225586");
			parameters.put("company", "ZF");
			parameters.put("department", "IT部門");
			parameters.put("logoImg", "https://zfcloud.cc/assets/logo02.jpg");
			// 設定背景圖片
			parameters.put("bg", bgInputStream);

			// 傳入所有可能的圖片 URL (最多8張)
			for (int i = 0; i < 8; i++) {
				InputStream imageIs = i < isList.size() ? isList.get(i) : null;
				
				String key = String.format("img%02d", i + 1);
				parameters.put(key, imageIs);
			}

			parameters.put("imgCount", isList.size());

			/**
			 * 2-2填充報表
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

			// 2-3.將JasperPrint以PDF形式輸出
			// 透過JasperExportManager工具類使用exportReportToPdfFile
			// 傳遞第一個參數JasperPrint對象
			// 傳遞第二個參數outputStream
			JasperExportManager.exportReportToPdfStream(print, outputStream);

		} catch (JRException e) {
			e.printStackTrace();
		} finally {
			// 最終關閉這個響應輸出流
			outputStream.close();
			mainInputStream.close();
		}

	}
	
	

	/**
	 * 產生參加證明，測試時不能使用 knife4j 或者 swagger
	 * 
	 * @param request
	 * @param response
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@GetMapping("certificate")
	public void generateCertificate(HttpServletResponse response) throws IOException {

		// 1.引入certificate(參加證明)  Jasper文件(模板)，並獲得inputStream
		Resource resource = new ClassPathResource("jasperTemplate/certificate.jasper");
		InputStream mainInputStream = resource.getInputStream();

		// 2.引入certificate(參加證明) 背景圖片，並獲得inputStream
		Resource bgResource = new ClassPathResource("jasperTemplate/certificate.jpg");
		InputStream bgInputStream = bgResource.getInputStream();

		// 3.透過response得到響應輸出流,不做設置直接響應
		ServletOutputStream outputStream = response.getOutputStream();

		// 4.準備數據,向Jasper文件填充數據
		try {

			Map<String, Object> parameters = new HashMap<>();
			// 1922511515324006402 中英文姓名
			// 1922512756607303682 英文姓名
			Member member = memberMapper.selectById(1922511515324006402L);

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

			// 設定 finalName
			parameters.put("finalName", finalName);
			// 設定背景圖片
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
			e.printStackTrace();
		} finally {
			// 最終關閉這個響應輸出流
			outputStream.close();
			mainInputStream.close();
			bgInputStream.close();
		}

	}

	/**
	 * 產生 Invoice，測試時不能使用 knife4j 或者 swagger
	 * 
	 * @param request
	 * @param response
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@GetMapping("conference_invoice")
	public void generateConferenceInvoice(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException, IOException {

		// 1.引入Invoice(繳費證明)  Jasper文件(模板) 及 子報表 的InputStream
		Resource resource = new ClassPathResource("jasperTemplate/conference_invoice.jasper");
		InputStream mainInputStream = resource.getInputStream();

		Resource subReportResource = new ClassPathResource("jasperTemplate/orderItems.jasper");
		InputStream subReportInputStream = subReportResource.getInputStream();

		// 2.引入Invoice(繳費證明) 背景圖片
		Resource bgResource = new ClassPathResource("jasperTemplate/conference_invoice.jpg");
		InputStream bgInputStream = bgResource.getInputStream();

		// 3.透過response得到響應輸出流
		ServletOutputStream outputStream = response.getOutputStream();

		// 4.拿到活動日期
		LocalDate date = LocalDate.of(2025, 11, 15);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d ,yyyy", Locale.ENGLISH);
		String eventDate = date.format(formatter);

		// 5.拿到流水編號
		String sequenceNo = String.format("%03d", 123);

		// 6.準備數據,向Jasper文件填充數據
		try {

			Map<String, Object> parameters = new HashMap<>();

			// 1922511515324006402 中英文姓名
			// 1922512756607303682 英文姓名
			parameters.put("finalName", "Abdul Hadi Anuar");
			parameters.put("eventDate", eventDate);
			parameters.put("sequenceNo", sequenceNo);
			parameters.put("totalAmount", BigDecimal.valueOf(300.00).setScale(2, RoundingMode.HALF_UP));
			parameters.put("contactEmail", "iopbs2025@gmail.com");
			// 設定背景圖片 及 子報表
			parameters.put("bg", bgInputStream);
			parameters.put("subReport", subReportInputStream);

			//準備訂單資料
			ArrayList<Orders> orderList = new ArrayList<>();
			for (int i = 0; i < 8; i++) {
				long amount = 35800;
				long currentAmount = amount + i;
				Orders tempOrder = new Orders();

				//台幣轉美金
				tempOrder.setTotalAmount(
						BigDecimal.valueOf(currentAmount).divide(BigDecimal.valueOf(32), 2, RoundingMode.HALF_UP));

				tempOrder.setItemsSummary("Registration Fee");
				orderList.add(tempOrder);
			}
			parameters.put("orderItems", orderList);

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
			e.printStackTrace();
		} finally {
			// 最終關閉這個響應輸出流
			outputStream.close();
			mainInputStream.close();
			bgInputStream.close();
		}

	}

	//	// 透過JavaBean List集合當作數據源填充數據,填入jasper文件中
	//	@GetMapping("/testJasper03")
	//	public void testPdf03(HttpServletRequest request, HttpServletResponse response)
	//			throws FileNotFoundException, IOException {
	//		// 1.引入Jasper文件
	//		Resource resource = new ClassPathResource("jasperTemplate/testReportJavaBean.jasper");
	//
	//		// 使用resource.getFile將他獲取到的檔案,變為一個File Class
	//		// 在透過FileInputStream的構造函數,將File當作參數傳入
	//		// 獲得一個檔案Input流
	//		FileInputStream fileInputStream = new FileInputStream(resource.getFile());
	//
	//		// 為響應頭設置檔名
	//		// response.setHeader("Content-Disposition", "attachment;
	//		// filename=yourFileName.pdf");
	//
	//		// 透過response得到響應輸出流,不做設置直接響應
	//		ServletOutputStream outputStream = response.getOutputStream();
	//
	//		// 2.創建JasperPrint,向Jasper文件填充數據
	//		try {
	//			// 務必要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
	//			// 第一個參數為: 文件輸入流 FileInputStream,準確來說是.jasper文件
	//			// 第二個參數為: 映射對象 Map 向模板中輸入的參數
	//			// 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據)
	//			// 地三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new JREmptyDataSource()來替代
	//
	//			Map<String, Object> params = new HashMap<>();
	//
	//			params.put("userName", "孫悟空");
	//			params.put("phone", "0985225586");
	//			params.put("company", "ZF");
	//			params.put("department", "IT部門");
	//
	//			// 構造JavaBean的數據源
	//			List<User> selectList = userMapper.selectList(null);
	//			for (User item : selectList) {
	//				System.out.println(item);
	//			}
	//			
	//			
	//			// 透過New 一個JRBeanCollectionDataSource,將list當作參數放入,可以直接將list當作數據源
	//			JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(selectList);
	//
	//			JasperPrint print = JasperFillManager.fillReport(fileInputStream, params, jrBeanCollectionDataSource);
	//
	//			// 3.將JasperPrint以PDF形式輸出
	//
	//			// 透過JasperExportManager工具類使用exportReportToPdfFile
	//			// 傳遞第一個參數JasperPrint對象
	//			// 傳遞第二個參數outputStream
	//			JasperExportManager.exportReportToPdfStream(print, outputStream);
	//
	//		} catch (JRException e) {
	//			e.printStackTrace();
	//		} finally {
	//			// 最終關閉這個響應輸出流
	//			outputStream.close();
	//			fileInputStream.close();
	//		}
	//
	//	}
	//
	//	// 透過JavaBean List集合當作數據源填充數據,並使用Group分組,填入jasper文件中
	//	@GetMapping("/testJasper04")
	//	public void testPdf04(HttpServletRequest request, HttpServletResponse response)
	//			throws FileNotFoundException, IOException {
	//		// 1.引入Jasper文件
	//		Resource resource = new ClassPathResource("jasperTemplate/testReportGroup.jasper");
	//
	//		// 使用resource.getFile將他獲取到的檔案,變為一個File Class
	//		// 在透過FileInputStream的構造函數,將File當作參數傳入
	//		// 獲得一個檔案Input流
	//		FileInputStream fileInputStream = new FileInputStream(resource.getFile());
	//
	//		// 為響應頭設置檔名
	//		// response.setHeader("Content-Disposition", "attachment;
	//		// filename=yourFileName.pdf");
	//
	//		// 透過response得到響應輸出流,不做設置直接響應
	//		ServletOutputStream outputStream = response.getOutputStream();
	//
	//		// 2.創建JasperPrint,向Jasper文件填充數據
	//		try {
	//			// 務必要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
	//			// 第一個參數為: 文件輸入流 FileInputStream,準確來說是.jasper文件
	//			// 第二個參數為: 映射對象 Map 向模板中輸入的參數
	//			// 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據)
	//			// 地三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new JREmptyDataSource()來替代
	//
	//			Map<String, Object> params = new HashMap<>();
	//
	//			params.put("userName", "孫悟空");
	//			params.put("phone", "0985225586");
	//			params.put("company", "ZF");
	//			params.put("department", "IT部門");
	//
	//			// 構造JavaBean的數據源
	//			List<User> selectList = userMapper.selectList(null);
	//			for (User item : selectList) {
	//				System.out.println(item);
	//			}
	//
	//			
	//
	//			// 透過New 一個JRBeanCollectionDataSource,將list當作參數放入,可以直接將list當作數據源
	//			JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(selectList);
	//
	//			JasperPrint print = JasperFillManager.fillReport(fileInputStream, params, jrBeanCollectionDataSource);
	//
	//			// 3.將JasperPrint以PDF形式輸出
	//
	//			// 透過JasperExportManager工具類使用exportReportToPdfFile
	//			// 傳遞第一個參數JasperPrint對象
	//			// 傳遞第二個參數outputStream
	//			JasperExportManager.exportReportToPdfStream(print, outputStream);
	//
	//		} catch (JRException e) {
	//			e.printStackTrace();
	//		} finally {
	//			// 最終關閉這個響應輸出流
	//			outputStream.close();
	//			fileInputStream.close();
	//		}
	//
	//	}
	//
	//	// 透過JavaBean List集合當作數據源填充數據,並使用Group分組,填入jasper文件中
	//	// 暫時無法使用
	//	@GetMapping("/testJasper05")
	//	public void testPdf05(HttpServletRequest request, HttpServletResponse response)
	//			throws FileNotFoundException, IOException {
	//		// 1.引入Jasper文件
	//		Resource resource = new ClassPathResource("jasperTemplate/testReportChart.jasper");
	//
	//		// 使用resource.getFile將他獲取到的檔案,變為一個File Class
	//		// 在透過FileInputStream的構造函數,將File當作參數傳入
	//		// 獲得一個檔案Input流
	//		FileInputStream fileInputStream = new FileInputStream(resource.getFile());
	//
	//		// 為響應頭設置檔名
	//		// response.setHeader("Content-Disposition", "attachment;
	//		// filename=yourFileName.pdf");
	//
	//		// 透過response得到響應輸出流,不做設置直接響應
	//		ServletOutputStream outputStream = response.getOutputStream();
	//
	//		// 2.創建JasperPrint,向Jasper文件填充數據
	//		try {
	//			// 務必要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
	//			// 第一個參數為: 文件輸入流 FileInputStream,準確來說是.jasper文件
	//			// 第二個參數為: 映射對象 Map 向模板中輸入的參數
	//			// 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據)
	//			// 地三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new JREmptyDataSource()來替代
	//
	//			Map<String, Object> params = new HashMap<>();
	//
	//			params.put("userName", "孫悟空");
	//			params.put("phone", "0985225586");
	//			params.put("company", "ZF");
	//			params.put("department", "IT部門");
	//
	//			// 構造JavaBean的數據源
	//			List<User> selectList = userMapper.selectList(null);
	//			for (User item : selectList) {
	//				System.out.println(item);
	//			}
	//
	//			
	//
	//			// 透過New 一個JRBeanCollectionDataSource,將list當作參數放入,可以直接將list當作數據源
	//			JRBeanCollectionDataSource jrBeanCollectionDataSource = new JRBeanCollectionDataSource(selectList);
	//
	//			JasperPrint print = JasperFillManager.fillReport(fileInputStream, params, jrBeanCollectionDataSource);
	//
	//			// 3.將JasperPrint以PDF形式輸出
	//
	//			// 透過JasperExportManager工具類使用exportReportToPdfFile
	//			// 傳遞第一個參數JasperPrint對象
	//			// 傳遞第二個參數outputStream
	//			JasperExportManager.exportReportToPdfStream(print, outputStream);
	//
	//		} catch (JRException e) {
	//			e.printStackTrace();
	//		} finally {
	//			// 最終關閉這個響應輸出流
	//			outputStream.close();
	//			fileInputStream.close();
	//		}
	//
	//	}

}