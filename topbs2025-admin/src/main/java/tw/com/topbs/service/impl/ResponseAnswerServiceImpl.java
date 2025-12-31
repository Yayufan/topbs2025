package tw.com.topbs.service.impl;

import tw.com.topbs.pojo.entity.ResponseAnswer;
import tw.com.topbs.mapper.ResponseAnswerMapper;
import tw.com.topbs.service.ResponseAnswerService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import net.sf.jasperreports.engine.design.events.CollectionElementRemovedEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * <p>
 * 表單回覆內容 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@Service
public class ResponseAnswerServiceImpl extends ServiceImpl<ResponseAnswerMapper, ResponseAnswer>
		implements ResponseAnswerService {

	@Override
	public List<ResponseAnswer> getAnswersByResponseId(Long responseId) {
		LambdaQueryWrapper<ResponseAnswer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ResponseAnswer::getFormResponseId, responseId);
		return baseMapper.selectList(queryWrapper);
	}

	@Override
	public Map<Long, ResponseAnswer> getAnswersKeyedByFieldId(Long responseId) {
		return this.getAnswersByResponseId(responseId)
				.stream()
				.collect(Collectors.toMap(ResponseAnswer::getFormFieldId, Function.identity()));
	}

	@Override
	public void deleteAnswerByResponseId(Long responseId) {
		LambdaQueryWrapper<ResponseAnswer> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(ResponseAnswer::getFormResponseId, responseId);
		baseMapper.delete(queryWrapper);
	}

}
