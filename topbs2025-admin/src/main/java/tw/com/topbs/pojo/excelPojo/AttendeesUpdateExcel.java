package tw.com.topbs.pojo.excelPojo;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class AttendeesUpdateExcel {

	 // 內部編號 - 唯一識別碼，用於匯入時資料庫查找。
    @ExcelProperty("參加者ID")
    private Long attendeesId;

    // 收據編號 - 這是唯一允許修改的欄位，使用者可以在此欄位填入新的收據號碼。
    @ExcelProperty("收據編號")
    private String receiptNo;

}
