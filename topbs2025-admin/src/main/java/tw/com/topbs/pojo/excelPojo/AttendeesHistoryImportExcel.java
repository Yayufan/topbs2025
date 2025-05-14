package tw.com.topbs.pojo.excelPojo;

import com.alibaba.excel.annotation.ExcelProperty;

import lombok.Data;

@Data
public class AttendeesHistoryImportExcel {

	@ExcelProperty("year")
	private Integer year;

	@ExcelProperty("id_card")
	private String idCard;

	@ExcelProperty("email")
	private String email;

	@ExcelProperty("name")
	private String name;

}
