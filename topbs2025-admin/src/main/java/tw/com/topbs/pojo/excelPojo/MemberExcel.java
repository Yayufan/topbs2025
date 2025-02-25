package tw.com.topbs.pojo.excelPojo;

import java.time.LocalDateTime;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class MemberExcel {
	
//	@ExcelProperty("會員ID")
//	private String memberId;

//	@ExcelProperty("社交帳號登入提供商")
//	private String provider;

//	@ExcelProperty("會員社交帳號UserId")
//	private String providerUserId;
	
	@ExcelProperty("會員編號")
	private String code;

	@ExcelProperty("會員姓名")
	private String name;
	
	@ExcelProperty("生日")
    private String birthday;

	@ExcelProperty("身分證字號")
	private String idCard;
	
	@ExcelProperty("性別")
	private String gender;
	
	@ExcelProperty("性別補充")
	private String genderOther;

	@ExcelProperty("服務單位")
	private String department;
	
	@ExcelProperty("職稱")
	private String jobTitle;
	
	@ExcelProperty("聯絡地址")
    private String contactAddress;
	

	@ExcelProperty("連絡電話")
    private String phone;

	@ExcelProperty("會員信箱")
	private String email;
	
//	@ExcelProperty("會員密碼")
//	private String password;
	
	
    @ExcelProperty("審核狀態")
    private String status;

    @ExcelProperty("創建時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ExcelProperty("最後更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ExcelProperty("最後更新者")
    private String updateBy;




	
}
