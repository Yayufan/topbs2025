package tw.com.topbs.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
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
import tw.com.topbs.manager.AttendeesProfileManager;
import tw.com.topbs.manager.AttendeesTagManager;
import tw.com.topbs.pojo.DTO.WalkInRegistrationDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagToAttendeesDTO;
import tw.com.topbs.pojo.VO.AttendeesStatsVO;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.utils.QrcodeUtil;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */

@Tag(name = "參加者API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/attendees")
public class AttendeesController {

	private final AttendeesProfileManager attendeeProfileManager;
	private final AttendeesTagManager attendeesTagManager;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一與會者")
	@SaCheckRole("super-admin")
	public R<AttendeesVO> getAttendees(@PathVariable("id") Long attendeesId) {
		AttendeesVO attendeesVO = attendeeProfileManager.getAttendeesVO(attendeesId);
		return R.ok(attendeesVO);
	}

	@GetMapping
	@Operation(summary = "查詢全部與會者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<AttendeesVO>> getAttendeesList() {
		List<AttendeesVO> attendeesList = attendeeProfileManager.getAttendeesVOList();
		return R.ok(attendeesList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢與會者(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<AttendeesVO>> getAttendeesPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Attendees> pageable = new Page<Attendees>(page, size);
		IPage<AttendeesVO> attendeesPage = attendeeProfileManager.getAttendeesVOPage(pageable);
		return R.ok(attendeesPage);
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除與會者")
	public R<Attendees> deleteAttendees(@PathVariable("id") Long attendeesId) {
		attendeeProfileManager.deleteAttendees(attendeesId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除與會者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeleteAttendees(@RequestBody List<Long> ids) {
		attendeeProfileManager.batchDeleteAttendees(ids);
		return R.ok();

	}

	@Operation(summary = "下載與會者excel列表")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("/download-excel")
	public void downloadExcel(HttpServletResponse response) throws IOException {
		attendeeProfileManager.downloadExcel(response);
	}

	/** 以下是跟Tag有關的Controller */

	@Operation(summary = "根據與會者ID 查詢與會者資料及他持有的標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@GetMapping("tag/{id}")
	public R<AttendeesTagVO> getAttendeesTagVOByAttendees(@PathVariable("id") Long attendeesId) {
		AttendeesTagVO attendeesTagVOByAttendees = attendeesTagManager.getAttendeesTagVO(attendeesId);
		return R.ok(attendeesTagVOByAttendees);

	}

	@Operation(summary = "根據條件 查詢與會者資料及他持有的標籤(分頁)")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("tag/pagination")
	public R<IPage<AttendeesTagVO>> getAllAttendeesTagVOByQuery(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String queryText) {

		Page<Attendees> pageInfo = new Page<>(page, size);
		IPage<AttendeesTagVO> attendeesPage;

		attendeesPage = attendeesTagManager.getAttendeesTagVOPageByQuery(pageInfo, queryText);

		return R.ok(attendeesPage);
	}

	@Operation(summary = "為與會者新增/更新/刪除 複數標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("tag")
	public R<Void> assignTagToAttendees(@Validated @RequestBody AddTagToAttendeesDTO addTagToAttendeesDTO) {
		attendeesTagManager.assignTagToAttendees(addTagToAttendeesDTO.getTargetTagIdList(),
				addTagToAttendeesDTO.getAttendeesId());
		return R.ok();

	}

	@Operation(summary = "現場登記(現場報名並簽到)")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@PostMapping("on-site")
	public R<CheckinRecordVO> walkInRegistration(@RequestBody @Valid WalkInRegistrationDTO walkInRegistrationDTO)
			throws IOException, Exception {
		CheckinRecordVO checkinRecordVO = attendeeProfileManager.walkInRegistration(walkInRegistrationDTO);
		return R.ok(checkinRecordVO);
	}

	@Operation(summary = "查詢與會者簽到的統計數據")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("stats")
	public R<AttendeesStatsVO> getAttendeesStatsVO() {
		return R.ok(attendeeProfileManager.getAttendeesStatsVO());
	}

	/** 跟QRcode產生有關 */
	@Operation(summary = "根據attendeesId 產生QRcode，需用Base64 解碼")
	@GetMapping(value = "/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
	public byte[] generateQRCode(@RequestParam Long attendeesId, @RequestParam(defaultValue = "250") int width,
			@RequestParam(defaultValue = "250") int height) throws Exception {

		byte[] qrCodeImage = QrcodeUtil.generateBase64QRCode(attendeesId.toString(), width, height);

		return qrCodeImage;

	}

}
