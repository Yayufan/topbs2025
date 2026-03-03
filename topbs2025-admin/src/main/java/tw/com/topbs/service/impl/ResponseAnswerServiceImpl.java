package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import tw.com.topbs.mapper.ResponseAnswerMapper;
import tw.com.topbs.pojo.BO.ResponseAnswerMatrixBO;
import tw.com.topbs.pojo.entity.ResponseAnswer;
import tw.com.topbs.service.ResponseAnswerService;

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
	public List<ResponseAnswer> searchAnswersByResponse(Long responseId) {
		return baseMapper.listByResponseId(responseId);
	}

	@Override
	public List<ResponseAnswer> searchAnswersByResponses(Collection<Long> responseIds) {
		return baseMapper.listByResponseIds(responseIds);
	}

	@Override
	public Map<Long, ResponseAnswer> searchAnswerMapByFieldId(Long responseId) {
		return this.searchAnswersByResponse(responseId)
				.stream()
				.collect(Collectors.toMap(ResponseAnswer::getFormFieldId, Function.identity()));
	}

	@Override
	public ResponseAnswerMatrixBO searchResponseAnswerMatrixBO(Collection<Long> responseIds) {
		return this.searchAnswersByResponses(responseIds)
				.stream()
				.collect(
						Collectors
								.collectingAndThen(
										Collectors.groupingBy(ResponseAnswer::getFormResponseId,
												Collectors.toMap(ResponseAnswer::getFormFieldId,
														ResponseAnswer::getAnswerValue, (v1, v2) -> v1 + ", " + v2)),
										ResponseAnswerMatrixBO::new // 這裡等同於 map -> new ResponseAnswerMatrixBO(map)
								));
	}

	@Override
	public void removeByResponse(Long responseId) {
		baseMapper.deleteByResponseId(responseId);
	}

	@Override
	public void removeByResponses(Collection<Long> responseIds) {
		baseMapper.deleteByResponseIds(responseIds);
	}

}
