package tw.com.topbs.pojo.DTO.putEntityDTO;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tw.com.topbs.enums.CommonStatusEnum;
import tw.com.topbs.enums.FormFieldTypeEnum;
import tw.com.topbs.pojo.DTO.FormFieldOptionDTO;
import tw.com.topbs.pojo.DTO.FormFieldValidationRuleDTO;
import tw.com.topbs.validation.annotation.ValidFormFieldOptions;
import tw.com.topbs.validation.constraint.HasFieldOptions;

@ValidFormFieldOptions
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PutFormFieldDTO implements HasFieldOptions {

	@Schema(description = "主鍵ID")
	private Long formFieldId;
	
	@Schema(description = "表單ID")
	private Long formId;

	@Schema(description = "text (簡答) , textarea(詳答) , number(數字) , email(信箱) , 	select (下拉式選單) ,"
			+ " radio(單選題) , rate(評分題) , checkbox(多選題) , date(日期題) , 	section(非問題,區塊段落) ,"
			+ " image(非問題,區塊圖片)")
	private FormFieldTypeEnum fieldType;

	@Schema(description = "問題文字")
	private String label;

	@Schema(description = "問題描述")
	private String description;

	@Schema(description = "輸入框提示文字 , 僅對 text 和 textarea 有效")
	private String placeholder;

	@Schema(description = "圖片儲存路徑")
	private String imageUrl;

	@Schema(description = "圖片說明文字")
	@TableField("image_caption")
	private String imageCaption;

	@Schema(description = "是否必填 , 0為false(不是必填) , 1為true(必填)")
	@NotNull
	private CommonStatusEnum isRequired;

	@Schema(description = "顯示順序 , 數字越小排的越前面")
	private Integer fieldOrder;

	@Schema(description = "選項資料")
	@Valid
	private FormFieldOptionDTO options;

	@Schema(description = "驗證規則")
	@Valid
	private FormFieldValidationRuleDTO validationRules;
	
}
