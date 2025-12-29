package tw.com.topbs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.enums.FormStatusEnum;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormDTO;
import tw.com.topbs.pojo.entity.Form;

/**
 * <p>
 * 自定義客制化表單 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-12-23
 */
public interface FormService extends IService<Form> {

	Form getForm(Long formId);

	/**
	 * 判斷目前是否有綁定簽退表單
	 * 
	 * @return
	 */
	boolean isCheckoutFormExist();

	/**
	 * 傳入當前表單 , 只有當 當前表單是否有要綁定簽退表單 以及 簽退表單已是否存在 的情況會返回true
	 * 
	 * @param form 當前要新增 或 修改的表單
	 * @return
	 */
	boolean isCheckoutFormExist(Form form);

	/**
	 * 根據表單狀態 及 查詢條件 , 獲取表單分頁
	 * 
	 * @param page           分頁對象
	 * @param formStatusEnum 發布狀態
	 * @param queryText      查詢文字
	 * @return
	 */
	IPage<Form> getFormPageByQuery(Page<Form> page, FormStatusEnum formStatusEnum, String queryText);

	Form addForm(AddFormDTO addForm);

	void updateForm(PutFormDTO putFormDTO);

	void deleteForm(Long formId);

}
