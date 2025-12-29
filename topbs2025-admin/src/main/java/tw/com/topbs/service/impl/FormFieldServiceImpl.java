package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormFieldConvert;
import tw.com.topbs.mapper.FormFieldMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.FormField;
import tw.com.topbs.service.FormFieldService;

/**
 * <p>
 * 表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
@Service
@RequiredArgsConstructor
public class FormFieldServiceImpl extends ServiceImpl<FormFieldMapper, FormField> implements FormFieldService {

	private final FormFieldConvert formFieldConvert;

	@Override
	public List<FormFieldVO> getFormFieldsByFormId(Long formId) {
		LambdaQueryWrapper<FormField> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(FormField::getFormId, formId);
		 List<FormFieldVO> voList = baseMapper.selectList(queryWrapper).stream().map(formField -> {
			return formFieldConvert.entityToVO(formField);
		}).toList();

		return voList;
	}

	@Override
	public FormField addFormField(AddFormFieldDTO addFormFieldDTO) {
		FormField formField = formFieldConvert.addDTOToEntity(addFormFieldDTO);
		baseMapper.insert(formField);
		return formField;
	}

	@Override
	public void updateFormField(PutFormFieldDTO putFormFieldDTO) {
		FormField formField = formFieldConvert.putDTOToEntity(putFormFieldDTO);
		baseMapper.updateById(formField);
	}

	@Override
	public void deleteFormField(Long formFieldId) {
		baseMapper.deleteById(formFieldId);
	}

}
