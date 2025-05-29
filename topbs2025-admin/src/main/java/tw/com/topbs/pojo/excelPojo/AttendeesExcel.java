package tw.com.topbs.pojo.excelPojo;

import java.time.LocalDateTime;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;

import lombok.Data;

@Data
// 每列高度為 100 pt（圖片建議 100~120pt 可完全顯示 200px 的圖）
@ContentRowHeight(100)
// 表示這一欄的寬度為「20 個字元」每個單位是約 1 個英文字的寬度，20 大致對應圖片寬度約 200px
@ColumnWidth(80 / 4)
public class AttendeesExcel {

	@ExcelProperty("參加者ID")
	private String attendeesId;

	@ExcelProperty("初次簽到時間")
	private LocalDateTime firstCheckinTime;

	@ExcelProperty("最後簽退時間")
	private LocalDateTime lastCheckoutTime;

	@ExcelProperty("會員ID")
	private String memberId;

	// Member 相關欄位

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
	
	@ExcelProperty(value = "簽到QRCode")
	private byte[] QRcodeImage;

}
