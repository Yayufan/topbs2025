package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
	public List<FormResponse> searchSubmissionsByForm(Long formId) {
		return baseMapper.listByFormId(formId);
	}
	
	@Override
	public IPage<FormResponse> searchSubmissionsByForm(IPage<FormResponse> pageInfo,Long formId) {
		return baseMapper.pageByFormId(pageInfo, formId);
	}

	@Override
	public List<FormResponse> searchSubmissionsByMember(Long formId, Long memberId) {
		return baseMapper.listByFormIdAndMemberId(formId, memberId);
	}

	@Override
	public FormResponse submit(AddFormResponseDTO formResponseDTO) {
		FormResponse formResponse = formResponseConvert.addDTOToEntity(formResponseDTO);
		baseMapper.insert(formResponse);
		return formResponse;
	}

	@Override
	public void removeByForm(Long formId) {
		baseMapper.deleteByFormId(formId);
	}



}
