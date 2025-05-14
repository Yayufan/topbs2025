package tw.com.topbs.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.entity.AttendeesHistory;
import tw.com.topbs.service.AttendeesHistoryService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 往年與會者名單 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-05-12
 */
@Tag(name = "往年與會者API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/attendees-history")
public class AttendeesHistoryController {

	private final AttendeesHistoryService attendeesHistoryService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一過往與會者")
	public R<AttendeesHistory> getAttendeesHistory(@PathVariable("id") Long attendeesHistoryId) {
		AttendeesHistory attendeesHistoryVO = attendeesHistoryService.getAttendeesHistory(attendeesHistoryId);
		return R.ok(attendeesHistoryVO);
	}

	@GetMapping
	@Operation(summary = "查詢全部過往與會者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<AttendeesHistory>> getAttendeesHistoryList() {
		List<AttendeesHistory> attendeesHistoryList = attendeesHistoryService.getAttendeesHistoryList();
		return R.ok(attendeesHistoryList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部過往與會者(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<AttendeesHistory>> getAttendeesHistoryPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<AttendeesHistory> pageable = new Page<AttendeesHistory>(page, size);
		IPage<AttendeesHistory> attendeesHistoryPage = attendeesHistoryService.getAttendeesHistoryPage(pageable);
		return R.ok(attendeesHistoryPage);
	}

	/**
	 * 清除所有往年與會者資料
	 */
	@Operation(summary = "清除所有往年與會者資料")
	@DeleteMapping("/clear")
	public R<Void> clearAttendeesHistory() {
		attendeesHistoryService.clearAllAttendeesHistory();
		return R.ok("已清除所有資料");
	}

	/**
	 * 下載Excel匯入模板
	 * 
	 * @param response
	 * @throws IOException
	 */
	@Operation(summary = "下載往年與會者匯入模板 (Excel)")
	@GetMapping("/excel-template")
	public void downloadTemplate(HttpServletResponse response) throws IOException {
		// 由 Service 生出 excel 模板內容 (byte array)
		attendeesHistoryService.generateImportTemplate(response);

	}

	/**
	 * 匯入往年與會者名單 (Excel檔)
	 * 
	 * @throws IOException
	 */
	@Operation(summary = "匯入往年與會者名單 (Excel)")
	@PostMapping("/import")
	public R<Void> importAttendeesHistory(@RequestParam("file") MultipartFile file) throws IOException {
		attendeesHistoryService.importAttendeesHistory(file);
		return R.ok("匯入成功");
	}

}
