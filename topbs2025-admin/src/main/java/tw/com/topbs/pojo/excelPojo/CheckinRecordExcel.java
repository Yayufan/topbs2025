package tw.com.topbs.pojo.excelPojo;

import java.time.LocalDateTime;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class CheckinRecordExcel {

	@ExcelProperty("主鍵ID")
	private String checkinRecordId;

	@ExcelProperty("簽到/退地點")
	private String location;

	// 1轉換成簽到,2轉換成簽退
	@ExcelProperty("動作類型")
	private String actionType;

	@ExcelProperty("簽到/退時間")
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime actionTime;

	@ExcelProperty("參加者ID")
	private String attendeesId;

	@ExcelProperty("會員ID")
	private String memberId;

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

	// checkinRecord 欄位
	@ExcelProperty("備註")
	private String remark;

}
