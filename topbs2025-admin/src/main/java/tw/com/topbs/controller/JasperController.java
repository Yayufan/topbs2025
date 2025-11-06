package tw.com.topbs.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/jasper")
public class JasperController {

	// 簡單的PDF
	@GetMapping("testJasper")
	public void testPdf(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException, IOException {

		// 1.引入Jasper文件
		Resource resource = new ClassPathResource("jasperTemplate/Blank_A4.jasper");

		// 使用resource.getFile將他獲取到的檔案,變為一個File Class
		// 在透過FileInputStream的構造函數,將File當作參數傳入
		// 獲得一個檔案Input流

		InputStream fileInputStream = resource.getInputStream();

		// 為響應頭設置檔名
		// response.setHeader("Content-Disposition", "attachment;
		// filename=yourFileName.pdf");

		// 透過response得到響應輸出流,不做設置直接響應
		ServletOutputStream outputStream = response.getOutputStream();

		// 2.創建JasperPrint,向Jasper文件填充數據
		try {
			// 務必要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
			// 第一個參數為: 文件輸入流 FileInputStream,準確來說是.jasper文件
			// 第二個參數為: 映射對象 Map 向模板中輸入的參數
			// 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據)
			// 地三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new JREmptyDataSource()來替代
			JasperPrint print = JasperFillManager.fillReport(fileInputStream, new HashMap<>(), new JREmptyDataSource());

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
			fileInputStream.close();
		}

	}

	// 透過Params傳遞參數,填入jasper文件中，測試時不能使用 knife4j 或者 swagger
	@GetMapping("testJasper02")
	public void testPdf02(HttpServletRequest request, HttpServletResponse response)
			throws FileNotFoundException, IOException {

		// 1.引入Jasper文件
		//		Resource resource = new ClassPathResource("jasperTemplate/testReportParams.jasper");
		Resource resource = new ClassPathResource("jasperTemplate/Blank_A4.jasper");

		// 2.引入背景圖片
		Resource bgResource = new ClassPathResource("jasperTemplate/background.jpg");

		// 使用resource.getFile將他獲取到的檔案,變為一個File Class
		// 在透過FileInputStream的構造函數,將File當作參數傳入
		// 獲得一個檔案Input流
		//		FileInputStream fileInputStream = new FileInputStream(resource.getFile());
		InputStream fileInputStream = resource.getInputStream();

		InputStream bgInputStream = bgResource.getInputStream();

		// 為響應頭設置檔名
		// response.setHeader("Content-Disposition", "attachment;
		// filename=yourFileName.pdf");

		// 透過response得到響應輸出流,不做設置直接響應
		ServletOutputStream outputStream = response.getOutputStream();

		//		List<String> imgList  = new ArrayList<>(
		//				""
		//				);
		List<String> imageUrls = List.of("https://iopbs2025.org.tw/_nuxt/10_Stryker.B-YSotAr.png",
				"https://iopbs2025.org.tw/_nuxt/11_Sumtage.BMKSE1an.png",
				"https://iopbs2025.org.tw/_nuxt/13_Amgen.BocFfSFi.png",
				"https://iopbs2025.org.tw/_nuxt/14_BW_LOGO.CJiSyvMf.png",
				"https://iopbs2025.org.tw/_nuxt/1_Cell_Trion._pmPMe7y.png",
				"https://iopbs2025.org.tw/_nuxt/2_Lotus.EBzwG3-V.png",
				"https://iopbs2025.org.tw/_nuxt/6_Eisai.DC94uV_T.png",
				"https://iopbs2025.org.tw/_nuxt/4_Gentek.DmKiuhev.png");
		
		// 2.創建JasperPrint,向Jasper文件填充數據
		try {
			// 務必要以三個參數來創建,儘管第三個參數數據源為空,不填寫編譯時也不會報錯,但最終PDF數據都會為空
			// 第一個參數為: 文件輸入流 FileInputStream,準確來說是.jasper文件
			// 第二個參數為: 映射對象 Map 向模板中輸入的參數
			// 第三個參數為: JasperDataSource 數據源(和Mysql數據源不同,這代表的是要填入的數據)
			// 地三個參數可以是Connection , 可以是Java Bean , 可以是Map,沒有時也務必new JREmptyDataSource()來替代

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

			JasperPrint print = JasperFillManager.fillReport(fileInputStream, parameters, new JREmptyDataSource());

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
			fileInputStream.close();
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