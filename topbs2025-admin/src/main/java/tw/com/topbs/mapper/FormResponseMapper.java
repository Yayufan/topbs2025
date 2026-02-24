package tw.com.topbs.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import tw.com.topbs.pojo.entity.FormResponse;

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
	 * 根據 formId 查找 表單的所有回覆 (分頁)
	 * 
	 * @param pageInfo
	 * @param formId
	 * @return
	 */
	default IPage<FormResponse> pageByFormId(IPage<FormResponse> pageInfo,Long formId) {
		LambdaQueryWrapper<FormResponse> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(FormResponse::getFormId, formId);
		return this.selectPage(pageInfo, queryWrapper);
	}
	

	/**
	 * 根據 memberId 查找 此會員對此表單的所有回覆
	 * 
	 * @param memberId
	 * @return
	 */
	default List<FormResponse> listByFormIdAndMemberId(Long formId, Long memberId) {
		LambdaQueryWrapper<FormResponse> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(FormResponse::getFormId, formId).eq(FormResponse::getMemberId, memberId);
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
