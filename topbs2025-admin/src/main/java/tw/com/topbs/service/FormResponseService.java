package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
	 * 根據formId 查詢此表單所有的回覆(分頁)
	 * 
	 * @param formId
	 * @return
	 */
	IPage<FormResponse> searchSubmissionsByForm(IPage<FormResponse> pageInfo,Long formId);
	
	/**
	 * 根據 memberId 查詢此會員對此表單所有的回覆
	 * 
	 * @param formId
	 * @param memberId
	 * @return
	 */
	List<FormResponse> searchSubmissionsByMember(Long formId,Long memberId);
	
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
