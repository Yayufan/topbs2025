package tw.com.topbs.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldOrderDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.FormField;

/**
 * <p>
 * 表單欄位 , 用於記錄某張自定義表單 , 具有哪些欄位及欄位設定 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface FormFieldService extends IService<FormField> {

	/**
	 * 根據formId拿到 表單 及其 所有欄位 , 也就是整體表單結構<br>
	 * 返回值 根據 FieldOrder 正序排列
	 * 
	 * @param formId
	 * @return 根據 FieldOrder 正序排列
	 */
	List<FormFieldVO> searchFormStructureByForm(Long formId);

	/**
	 * 新增表單欄位,第一次創建一定不會有圖片
	 * 
	 * @param addFormFieldDTO
	 * @return
	 */
	FormField add(AddFormFieldDTO addFormFieldDTO);
	
	/**
	 * 複製表單欄位
	 * 
	 * @param addFormFieldDTO
	 * @return
	 */
	FormField copy(AddFormFieldDTO addFormFieldDTO);

	/**
	 * 更新表單欄位,可能會帶上上傳圖片
	 * 
	 * @param file
	 * @param putFormFieldDTO
	 */
	FormField modify(MultipartFile file, PutFormFieldDTO putFormFieldDTO);

	/**
	 * 批量更新，表單欄位排序值
	 * @param putFormFieldOrderDTOList
	 */
	void batchModifyOrder(List<PutFormFieldOrderDTO> putFormFieldOrderDTOList);
	
	/**
	 * 刪除表單欄位,會連帶刪除檔案(圖檔)
	 * 
	 * @param formFieldId
	 */
	void remove(Long formFieldId);

}
