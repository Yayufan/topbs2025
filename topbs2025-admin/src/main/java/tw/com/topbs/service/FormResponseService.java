package tw.com.topbs.service;

import java.util.List;

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
	 * 根據formId 查詢此表單所有的回覆
	 * 
	 * @param formId
	 * @return
	 */
	List<FormResponse> searchSubmissionsByForm(Long formId);
	
	/**
	 * 新增 表單回覆
	 * 
	 * @param formResponseDTO
	 * @return
	 */
	FormResponse submit(AddFormResponseDTO formResponseDTO);

	
	/**
	 * 根據 表單ID 刪除對應的表單回覆
	 * 
	 * @param formId 表單ID 
	 */
	void removeByForm(Long formId);

}
