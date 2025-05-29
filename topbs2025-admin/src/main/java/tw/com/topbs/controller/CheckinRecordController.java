package tw.com.topbs.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.UndoCheckinDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutCheckinRecordDTO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.service.CheckinRecordService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 簽到退紀錄 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-05-07
 */
@Tag(name = "簽到記錄API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/checkin-record")
public class CheckinRecordController {

	private final CheckinRecordService checkinRecordService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一簽到/退紀錄")
	public R<CheckinRecordVO> getCheckinRecord(@PathVariable("id") Long checkinRecordId) {
		CheckinRecordVO checkinRecordVO = checkinRecordService.getCheckinRecord(checkinRecordId);
		return R.ok(checkinRecordVO);
	}

	@GetMapping
	@Operation(summary = "查詢全部簽到/退紀錄")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<CheckinRecordVO>> getCheckinRecordList() {
		List<CheckinRecordVO> checkinRecordVOList = checkinRecordService.getCheckinRecordList();
		return R.ok(checkinRecordVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部簽到/退紀錄(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<CheckinRecordVO>> getCheckinRecordPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<CheckinRecord> pageable = new Page<CheckinRecord>(page, size);
		IPage<CheckinRecordVO> checkinRecordVOPage = checkinRecordService.getCheckinRecordPage(pageable);
		return R.ok(checkinRecordVOPage);
	}

	@PostMapping
	@Operation(summary = "新增單一簽到/退紀錄")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<CheckinRecordVO> saveCheckinRecord(@RequestBody @Valid AddCheckinRecordDTO addCheckinRecordDTO) {
		CheckinRecordVO checkinRecord = checkinRecordService.addCheckinRecord(addCheckinRecordDTO);
		return R.ok(checkinRecord);
	}
	
	@PutMapping("undo-checkin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "撤銷最後一筆簽到紀錄")
	@SaCheckRole("super-admin")
	public R<CheckinRecord> undoCheckin(@RequestBody @Valid UndoCheckinDTO undoCheckinDTO) {
		checkinRecordService.undoLastCheckin(undoCheckinDTO.getAttendeesId());
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "修改簽到/退紀錄")
	@SaCheckRole("super-admin")
	public R<CheckinRecord> updateCheckinRecord(@RequestBody @Valid PutCheckinRecordDTO putCheckinRecordDTO) {
		checkinRecordService.updateCheckinRecord(putCheckinRecordDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除簽到/退紀錄")
	public R<CheckinRecord> deleteCheckinRecord(@PathVariable("id") Long checkinRecordId) {
		checkinRecordService.deleteCheckinRecord(checkinRecordId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除簽到/退紀錄")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeleteCheckinRecord(@RequestBody List<Long> ids) {
		checkinRecordService.deleteCheckinRecordList(ids);
		return R.ok();

	}
	
	@Operation(summary = "下載簽到/退紀錄 excel列表")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("/download-excel")
	public void downloadExcel(HttpServletResponse response) throws IOException {
		checkinRecordService.downloadExcel(response);
	}

}
