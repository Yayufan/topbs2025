package tw.com.topbs.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormFieldConvert;
import tw.com.topbs.mapper.FormFieldMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldOrderDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.FormField;
import tw.com.topbs.service.FormFieldService;
import tw.com.topbs.utils.S3Util;

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

	private static final String BASE_PATH = "form/";

	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	private final S3Util s3Util;
	private final FormFieldConvert formFieldConvert;

	@Override
	public List<FormFieldVO> searchFormStructureByForm(Long formId) {
		List<FormFieldVO> voList = baseMapper.listByFormId(formId).stream().map(formField -> {
			return formFieldConvert.entityToVO(formField);
		}).toList();

		return voList;
	}

	@Override
	public FormField add(AddFormFieldDTO addFormFieldDTO) {
		FormField formField = formFieldConvert.addDTOToEntity(addFormFieldDTO);
		baseMapper.insert(formField);
		return formField;
	}

	@Override
	public FormField copy(AddFormFieldDTO addFormFieldDTO) {
		FormField formField = formFieldConvert.addDTOToEntity(addFormFieldDTO);

		// 這邊在後續開發也要注意 , 複製在option.choices 裡面的id 也會重複 , 注意有沒有衝突問題
		// 如果複製的問題剛好有圖片 , 則圖片也要進行複製後 , 重新set圖片路徑
		if (StringUtils.isNotBlank(formField.getImageUrl())) {

		}

		baseMapper.insert(formField);
		return formField;
	}

	@Override
	public FormField modify(MultipartFile file, PutFormFieldDTO putFormFieldDTO) {

		System.out.println("此次轉換數據:" + putFormFieldDTO);

		// 1.如果有傳檔案
		if (file != null) {

			// 1-1 優先查找舊資料 , 要移除沒在使用的檔案
			FormField oldFormField = baseMapper.selectById(putFormFieldDTO.getFormFieldId());
			// 這邊使用寬鬆刪除,也就是ImageUrl 如果為null 或為 空字串 , 自動忽略
			s3Util.removeFileIfPresent(bucketName, oldFormField.getImageUrl());

			// 1-2 上傳新檔案,拿到DB儲存路徑
			String dbUrl = s3Util.upload(BASE_PATH + putFormFieldDTO.getFormId(), file.getOriginalFilename(), file);

			// 1-3 將DB儲存路徑,放到 currentFormField
			putFormFieldDTO.setImageUrl(dbUrl);

		}

		// 2.轉換資料,並更新
		FormField formField = formFieldConvert.putDTOToEntity(putFormFieldDTO);
		baseMapper.updateById(formField);

		return formField;

	}

	@Transactional
	@Override
	public void batchModifyOrder(List<PutFormFieldOrderDTO> putFormFieldOrderDTOList) {

		// 1. 一次性查出這些 ID 目前在資料庫的完整資料 (1 條 SQL)
		List<Long> ids = putFormFieldOrderDTOList.stream().map(PutFormFieldOrderDTO::getFormFieldId).toList();
		List<FormField> existingEntities = baseMapper.selectBatchIds(ids);

		// 2. 將 DTO 的新順序對應到實體上
		// 先做成 Map 方便查找
		Map<Long, Integer> orderMap = putFormFieldOrderDTOList.stream()
				.collect(Collectors.toMap(PutFormFieldOrderDTO::getFormFieldId, PutFormFieldOrderDTO::getFieldOrder));

		for (FormField entity : existingEntities) {
			Integer newOrder = orderMap.get(entity.getFormFieldId());
			if (newOrder != null) {
				// 只修改 fieldOrder，其他的 options, validationRules 維持從資料庫查出來的原樣
				entity.setFieldOrder(newOrder);
			}
		}

		// 3. 批量更新
		this.updateBatchById(existingEntities);
	}

	@Override
	public void remove(Long formFieldId) {

		// 1.優先查找舊資料 , 要移除沒在使用的檔案(圖檔)
		FormField oldFormField = baseMapper.selectById(formFieldId);

		// 2.這邊使用寬鬆刪除,也就是ImageUrl 如果為null 或為 空字串 , 自動忽略
		s3Util.removeFileIfPresent(bucketName, oldFormField.getImageUrl());

		// 3.刪除資料本身
		baseMapper.deleteById(formFieldId);
	}

	@Override
	public void removeByForm(Long formId) {
		// 1.優先查找舊資料 , 要移除沒在使用的檔案(圖檔)
		List<FormField> listByFormId = baseMapper.listByFormId(formId);
		
		// 2.遍歷使用寬鬆刪除,也就是ImageUrl 如果為null 或為 空字串 , 自動忽略
		for (FormField formField : listByFormId) {
			s3Util.removeFileIfPresent(bucketName, formField.getImageUrl());
		}
		
		// 3.刪除所有符合的資料
		baseMapper.deleteByFormId(formId);
	}

}
