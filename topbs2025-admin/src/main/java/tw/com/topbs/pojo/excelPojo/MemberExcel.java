package tw.com.topbs.pojo.excelPojo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class MemberExcel {

	@ExcelProperty("頭銜")
	private String title;

	@ExcelProperty("名字")
	private String firstName;

	@ExcelProperty("姓氏")
	private String lastName;

	@ExcelProperty("護照號碼 OR 台灣身分證字號")
	private String idCard;

	@ExcelProperty("中文姓名")
	private String chineseName;

	@ExcelProperty("E-Mail")
	private String email;

	@ExcelProperty("單位(所屬的機構)")
	private String affiliation;

	@ExcelProperty("職稱")
	private String jobTitle;

	@ExcelProperty("國家")
	private String country;

	@ExcelProperty("匯款帳號-後五碼(台灣)")
	private String remitAccountLast5;

	@ExcelProperty("電話號碼")
	private String phone;

	// Entity中為Integer , Excel 為String 
	@ExcelProperty("會員資格")
	private String category;

	@ExcelProperty("資格的補充")
	private String categoryExtra;

	@ExcelProperty("收據抬頭統編")
	private String receipt;

	@ExcelProperty("餐食調查")
	private String food;

	@ExcelProperty("飲食禁忌")
	private String foodTaboo;

	@ExcelProperty("群組代號")
	private String groupCode;
	
	@ExcelProperty("主報名者(master),子報名者(slave)")
	private String groupRole;

	@ExcelProperty("註冊時間")
	private LocalDateTime createDate;
	
	// Entity中為Integer , Excel 為String 
	@ExcelProperty("付款狀態")
	private String status;

	@ExcelProperty("註冊費金額")
	private BigDecimal registrationFee;
	
}
