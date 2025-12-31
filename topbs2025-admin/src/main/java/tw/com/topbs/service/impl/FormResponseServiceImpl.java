package tw.com.topbs.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormResponseConvert;
import tw.com.topbs.mapper.FormResponseMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormResponseDTO;
import tw.com.topbs.pojo.entity.FormResponse;
import tw.com.topbs.service.FormResponseService;

/**
 * <p>
 * 表單回覆紀錄 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@Service
@RequiredArgsConstructor
public class FormResponseServiceImpl extends ServiceImpl<FormResponseMapper, FormResponse>
		implements FormResponseService {

	private final FormResponseConvert formResponseConvert;

	@Override
	public FormResponse addFormResponse(AddFormResponseDTO formResponseDTO) {
		FormResponse formResponse = formResponseConvert.addDTOToEntity(formResponseDTO);
		baseMapper.insert(formResponse);

		return formResponse;
	}

	@Override
	public void deleteFormResponse(Long formResponseId) {
		baseMapper.deleteById(formResponseId);

	}

}
