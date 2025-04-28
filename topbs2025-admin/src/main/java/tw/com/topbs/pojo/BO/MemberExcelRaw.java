package tw.com.topbs.pojo.BO;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class MemberExcelRaw {

	@ExcelProperty("頭銜 - 前墜詞")
	private String title;

	@ExcelProperty("名字, 華人的名在後  , 外國人的名在前")
	private String firstName;

	@ExcelProperty("姓氏, 華人的姓氏在前, 外國人的姓氏在後")
	private String lastName;

	@ExcelProperty("同時作為護照號碼 和 台灣身分證字號使用")
	private String idCard;

	@ExcelProperty("中文姓名，外國人非必填，台灣人必填")
	private String chineseName;

	@ExcelProperty("E-Mail")
	private String email;

	@ExcelProperty("單位(所屬的機構)")
	private String affiliation;

	@ExcelProperty("職稱")
	private String jobTitle;

	@ExcelProperty("國家")
	private String country;

	@ExcelProperty("匯款帳號-後五碼  台灣會員使用")
	private String remitAccountLast5;

	@ExcelProperty("電話號碼,這邊要使用 國碼-號碼")
	private String phone;

	// Entity中為Integer , Excel最終 為String 
	@ExcelProperty("用於分類會員資格, 1為 Member ，2為 Others ，3為Non-Member，4為MVP")
	private Integer category;

	@ExcelProperty("會員資格的身份補充")
	private String categoryExtra;

	@ExcelProperty("收據抬頭統編")
	private String receipt;

	@ExcelProperty("餐食調查，填寫葷 或 素")
	private String food;

	@ExcelProperty("飲食禁忌")
	private String foodTaboo;

	@ExcelProperty("群組代號, 用UUID randomUUID() 產生")
	private String groupCode;

	@ExcelProperty("當如果今天member具有群組, 那麼用這個確認他是主報名者 master,還是子報名者 slave , 這也是讓子報名者更換成主報名者的機制")
	private String groupRole;

	// Entity中為Integer , Excel最終為String 
	@ExcelProperty("0為未付款，2為已付款，3為付款失敗")
	private Integer status;

}
