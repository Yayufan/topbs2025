package tw.com.topbs.pojo.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tw.com.topbs.enums.FormFieldTypeEnum;

/**
 * <p>
 * 表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@Getter
@Setter
@TableName("form_field")
@Schema(name = "FormField", description = "表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定")
@ToString
public class FormField implements Serializable {

	private static final long serialVersionUID = 1L;

	@Schema(description = "主鍵ID")
	@TableId("form_field_id")
	private Long formFieldId;

	@Schema(description = "表單ID")
	@TableField("form_id")
	private Long formId;

	@Schema(description = "text (簡答) , textarea(詳答) , number(數字) , email(信箱) , 	select (下拉式選單) ,"
			+ " radio(單選題) , rate(評分題) , checkbox(多選題) , date(日期題) , 	section(非問題,區塊段落) ,"
			+ " image(非問題,區塊圖片)")
	@TableField("field_type")
	private FormFieldTypeEnum fieldType;

	@Schema(description = "問題文字")
	@TableField("label")
	private String label;

	@Schema(description = "問題描述")
	@TableField("description")
	private String description;

	@Schema(description = "輸入框提示文字 , 僅對 text 和 textarea 有效")
	@TableField("placeholder")
	private String placeholder;

	@Schema(description = "圖片儲存路徑")
	@TableField("image_url")
	private String imageUrl;

	@Schema(description = "圖片說明文字")
	@TableField("image_caption")
	private String imageCaption;

	@Schema(description = "是否必填 , 0為false(不是必填) , 1為true(必填)")
	@TableField("is_required")
	private Integer isRequired;

	@Schema(description = "顯示順序 , 數字越小排的越前面")
	@TableField("field_order")
	private Integer fieldOrder;

	@Schema(description = "選項資料")
	@TableField(value="options",updateStrategy = FieldStrategy.ALWAYS)
	private String options;

	@Schema(description = "驗證規則")
	@TableField(value = "validation_rules",updateStrategy = FieldStrategy.ALWAYS)
	private String validationRules;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createDate;

	@Schema(description = "最後修改者")
	@TableField(value = "update_by",fill = FieldFill.UPDATE)
	private String updateBy;

	@Schema(description = "最後修改時間")
	@TableField(value = "update_date", fill = FieldFill.UPDATE)
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateDate;

	@Schema(description = "邏輯刪除,預設為0活耀,1為刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;
}
