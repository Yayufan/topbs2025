package tw.com.topbs.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.pojo.entity.ResponseAnswer;

/**
 * <p>
 * 表單回覆內容 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface ResponseAnswerMapper extends BaseMapper<ResponseAnswer> {

	/**
	 * 根據 formResponseId 獲取 符合的 ResponseAnswer 列表
	 * 
	 * @param formResponseId
	 * @return
	 */
	default List<ResponseAnswer> listByResponseId(Long formResponseId) {
		LambdaQueryWrapper<ResponseAnswer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ResponseAnswer::getFormResponseId, formResponseId);
		return this.selectList(queryWrapper);
	}

	/**
	 * 根據 formResponseIds 獲取 符合的 ResponseAnswer 列表<br>
	 * 如果集合為空,返回空列表,不報錯
	 * 
	 * @param formResponseIds 表單回覆Ids
	 * @return
	 */
	default List<ResponseAnswer> listByResponseIds(Collection<Long> formResponseIds) {

		if (formResponseIds.isEmpty()) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<ResponseAnswer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.in(ResponseAnswer::getFormResponseId, formResponseIds);

		return this.selectList(queryWrapper);
	}

	/**
	 * 根據 formResponseId 刪除符合的 ResponseAnswer
	 * 
	 * @param formResponseId
	 */
	default void deleteByResponseId(Long formResponseId) {
		LambdaQueryWrapper<ResponseAnswer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ResponseAnswer::getFormResponseId, formResponseId);
		this.delete(queryWrapper);
	}

	/**
	 * 根據 formResponseIds 刪除符合的 ResponseAnswer<br>
	 * 如果集合為空,直接返回,不報錯
	 * 
	 * @param formResponseIds
	 */
	default void deleteByResponseIds(Collection<Long> formResponseIds) {
		if (formResponseIds.isEmpty()) {
			return;
		}

		LambdaQueryWrapper<ResponseAnswer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.in(ResponseAnswer::getFormResponseId, formResponseIds);
		this.delete(queryWrapper);
	}

}
