package tw.com.topbs.pojo.excelPojo;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class AttendeeUpdateTemplateExcel {

	 // 內部編號 - 唯一識別碼，用於匯入時資料庫查找。
    @ExcelProperty("attendeesId")
    private String attendeesId;

    // 姓名 - 識別欄位，不可修改。
    @ExcelProperty("name")
    private String name;
    
	@ExcelProperty("idCard")
	private String idCard;

    // Email - 識別欄位，不可修改。
    @ExcelProperty("email")
    private String email;

    // 序號 - 識別欄位，不可修改。
    @ExcelProperty("sequenceNo")
    private Integer sequenceNo;

    // 收據編號 - 這是唯一允許修改的欄位，使用者可以在此欄位填入新的收據號碼。
    @ExcelProperty("收據編號 (唯一可修改欄位)")
    private String receiptNo;

    // 備註: 實際匯入邏輯應忽略對「請勿修改」欄位的更改。
}
