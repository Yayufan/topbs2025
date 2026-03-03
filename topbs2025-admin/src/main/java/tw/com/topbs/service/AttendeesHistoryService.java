package tw.com.topbs.service;

import tw.com.topbs.pojo.entity.AttendeesHistory;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>
 * 往年與會者名單 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
public interface AttendeesHistoryService extends IService<AttendeesHistory> {
	/**
	 * 根據 attendeesHistoryId 獲取過往與會者
	 * 
	 * @param attendeesHistoryId
	 * @return
	 */
	AttendeesHistory getAttendeesHistory(Long attendeesHistoryId);

	/**
	 * 查詢所有過往與會者
	 * 
	 * @return
	 */
	List<AttendeesHistory> getAttendeesHistoryList();

	/**
	 * 查詢所有過往與會者(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<AttendeesHistory> getAttendeesHistoryPage(Page<AttendeesHistory> page);

	/**
	 * 根據年份 和 (ID card 或者 email)查詢往年與會者
	 * 
	 * @param year
	 * @param idCard
	 * @param email
	 * @return
	 */
	Boolean existsAttendeesHistory(Integer year, String idCard, String email);

	/**
	 * 產生 往年與會者 的匯入Excel模板
	 * 
	 * @param response
	 * @throws IOException
	 */
	void generateImportTemplate(HttpServletResponse response) throws IOException;

	/**
	 * excel匯入，需要依照模板上傳
	 * 
	 * @param file
	 * @throws IOException
	 */
	void importAttendeesHistory(MultipartFile file) throws IOException;

	/**
	 * 清除過往與會者資料，這個是真實刪除，因為這種資料都是舊資料(臨時資料)
	 * 
	 */
	void clearAllAttendeesHistory();
	

}
