package tw.com.topbs.mapper;

import tw.com.topbs.pojo.entity.FormResponse;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 表單回覆紀錄 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface FormResponseMapper extends BaseMapper<FormResponse> {

	/** 
	 * 根據 formId 查找 表單的所有回覆
	 * 
	 * @param formId
	 * @return
	 */
	default List<FormResponse> listByFormId(Long formId) {
		LambdaQueryWrapper<FormResponse> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(FormResponse::getFormId, formId);
		return this.selectList(queryWrapper);
	}
	
	/**
	 * 根據 formId 刪除 表單的所有回覆
	 * 
	 * @param formId
	 */
	default void deleteByFormId(Long formId) {
		LambdaQueryWrapper<FormResponse> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(FormResponse::getFormId, formId);
		this.delete(queryWrapper);
	};

}
