package tw.com.topbs.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.pojo.entity.FormField;

/**
 * <p>
 * 表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface FormFieldMapper extends BaseMapper<FormField> {

	/**
	 * 根據 formId 查找相對應的表單欄位, 並以欄位排序號 小 -> 大 排序
	 * 
	 * @param formId
	 * @return
	 */
	default List<FormField> listByFormId(Long formId){
		// 根據formId 查找相對應的表單欄位 , 並以欄位排序號 小 -> 大 排序
		LambdaQueryWrapper<FormField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(FormField::getFormId, formId).orderByAsc(FormField::getFieldOrder).orderByAsc(FormField::getFormFieldId);
		return this.selectList(queryWrapper);
	}
}
