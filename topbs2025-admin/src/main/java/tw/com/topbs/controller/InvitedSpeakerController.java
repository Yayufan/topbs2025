package tw.com.topbs.controller;

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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddInvitedSpeakerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutInvitedSpeakerDTO;
import tw.com.topbs.pojo.entity.InvitedSpeaker;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.InvitedSpeakerService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 受邀請的講者，可能是講者，可能是座長 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-04-23
 */

@Tag(name = "受邀講者API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/invited-speaker")
public class InvitedSpeakerController {

	private final MemberService memberService;
	private final InvitedSpeakerService invitedSpeakerService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一受邀講者紀錄")
	public R<InvitedSpeaker> getInvitedSpeaker(@PathVariable("id") Long invitedSpeakerId) {
		InvitedSpeaker invitedSpeaker = invitedSpeakerService.getInvitedSpeaker(invitedSpeakerId);
		return R.ok(invitedSpeaker);
	}

	@GetMapping
	@Operation(summary = "查詢全部受邀講者紀錄")
	public R<List<InvitedSpeaker>> getInvitedSpeakerList() {
		List<InvitedSpeaker> invitedSpeakerList = invitedSpeakerService.getAllInvitedSpeaker();
		return R.ok(invitedSpeakerList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部受邀講者紀錄(分頁)")
	public R<IPage<InvitedSpeaker>> getInvitedSpeakerPage(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String queryText) {
		Page<InvitedSpeaker> pageable = new Page<InvitedSpeaker>(page, size);
		IPage<InvitedSpeaker> invitedSpeakerPage = invitedSpeakerService.getInvitedSpeakerPage(pageable, queryText);
		return R.ok(invitedSpeakerPage);
	}

	@PostMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "data", description = "JSON 格式的檔案資料", required = true, schema = @Schema(implementation = AddInvitedSpeakerDTO.class)) })
	@SaCheckRole("super-admin")
	@Operation(summary = "新增受邀講者，不與會員功能連動，謹慎使用")
	public R<Void> saveInvitedSpeaker(@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam("data") String jsonData) throws JsonMappingException, JsonProcessingException {

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		AddInvitedSpeakerDTO addInvitedSpeakerDTO = objectMapper.readValue(jsonData, AddInvitedSpeakerDTO.class);

		invitedSpeakerService.addInvitedSpeaker(file, addInvitedSpeakerDTO);
		return R.ok();
	}

	@PutMapping(value = "owner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "修改受邀講者，講師自身修改", description = "請使用formData包裝,兩個key <br>"
			+ "1.data(value = PutInvitedSpeakerDTO(json))<br>" + "2.file(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> updateInvitedSpeakerByOwner(
			@RequestPart("data") @Schema(name = "data", implementation = PutInvitedSpeakerDTO.class) String jsonData,
			@RequestPart("file") MultipartFile file) throws JsonMappingException, JsonProcessingException {

		// 1.將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		PutInvitedSpeakerDTO putInvitedSpeakerDTO = objectMapper.readValue(jsonData, PutInvitedSpeakerDTO.class);

		// 2.根據token 拿取本人的數據，如果非本人直接報錯
		Member memberCache = memberService.getMemberInfo();
		if (!memberCache.getMemberId().equals(putInvitedSpeakerDTO.getMemberId())) {
			return R.fail("The Token is not the user's own and cannot retrieve non-user's information.");
		}

		// 3.如果是本身則直接新增
		invitedSpeakerService.updateInvitedSpeakerHimself(file, putInvitedSpeakerDTO);
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "data", description = "JSON 格式的檔案資料", required = true, schema = @Schema(implementation = PutInvitedSpeakerDTO.class)) })
	@Operation(summary = "修改受邀講者")
	@SaCheckRole("super-admin")
	public R<Void> updateInvitedSpeaker(@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam("data") String jsonData) throws JsonMappingException, JsonProcessingException {

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		PutInvitedSpeakerDTO putInvitedSpeakerDTO = objectMapper.readValue(jsonData, PutInvitedSpeakerDTO.class);

		invitedSpeakerService.updateInvitedSpeaker(file, putInvitedSpeakerDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除受邀講者")
	public R<InvitedSpeaker> deleteInvitedSpeaker(@PathVariable("id") Long invitedSpeakerId) {
		invitedSpeakerService.deleteInvitedSpeaker(invitedSpeakerId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除受邀講者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeleteInvitedSpeaker(@RequestBody List<Long> ids) {
		invitedSpeakerService.deleteInvitedSpeakerList(ids);
		return R.ok();

	}

}
