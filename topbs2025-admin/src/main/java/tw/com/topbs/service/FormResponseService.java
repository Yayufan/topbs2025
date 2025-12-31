package tw.com.topbs.service;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormResponseDTO;
import tw.com.topbs.pojo.entity.FormResponse;

/**
 * <p>
 * 表單回覆紀錄 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface FormResponseService extends IService<FormResponse> {

	/**
	 * 新增 表單回覆
	 * 
	 * @param formResponseDTO
	 * @return
	 */
	FormResponse addFormResponse(AddFormResponseDTO formResponseDTO);

	/**
	 * 刪除 表單回覆
	 * 
	 * @param formResponseId
	 */
	void deleteFormResponse(Long formResponseId);

}
