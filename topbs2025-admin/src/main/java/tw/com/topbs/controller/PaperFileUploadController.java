package tw.com.topbs.controller;

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

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.PaperFileUploadService;
import tw.com.topbs.utils.R;

@Tag(name = "稿件附件API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paper-file-upload")
public class PaperFileUploadController {

	private final PaperFileUploadService paperFileUploadService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一稿件附件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<PaperFileUpload> getPaperFileUpload(@PathVariable("id") Long paperFileUploadId) {
		PaperFileUpload paperFileUpload = paperFileUploadService.getPaperFileUpload(paperFileUploadId);
		return R.ok(paperFileUpload);
	}

	@GetMapping
	@Operation(summary = "查詢全部稿件附件")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<List<PaperFileUpload>> getUserList() {
		List<PaperFileUpload> paperFileUploadList = paperFileUploadService.getPaperFileUploadList();
		return R.ok(paperFileUploadList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部稿件附件(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<IPage<PaperFileUpload>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<PaperFileUpload> pageable = new Page<PaperFileUpload>(page, size);
		IPage<PaperFileUpload> paperFileUploadPage = paperFileUploadService.getPaperFileUploadPage(pageable);
		return R.ok(paperFileUploadPage);
	}

//	@PostMapping
//	@Operation(summary = "新增單一稿件附件")
//	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
//	public R<PaperFileUpload> savePaperFileUpload(@RequestBody @Valid AddPaperFileUploadDTO addPaperFileUploadDTO) {
//		paperFileUploadService.addPaperFileUpload(addPaperFileUploadDTO);
//		return R.ok();
//	}

//	@PutMapping
//	@Parameters({
//			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
//	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
//	@Operation(summary = "修改稿件附件")
//	public R<PaperFileUpload> updatePaperFileUpload(@RequestBody @Valid PutPaperFileUploadDTO putPaperFileUploadDTO) {
//		return R.ok();
//	}

//	@DeleteMapping("{id}")
//	@Parameters({
//			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
//	@Operation(summary = "刪除稿件附件")
//	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
//	public R<PaperFileUpload> deletePaperFileUpload(@PathVariable("id") Long paperFileUploadId) {
//		paperFileUploadService.deletePaperFile(paperFileUploadId);
//		return R.ok();
//	}

	@DeleteMapping
	@Operation(summary = "批量刪除稿件附件")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> batchDeletePaperFileUpload(@RequestBody List<Long> ids) {
		paperFileUploadService.deletePaperFileUploadList(ids);
		return R.ok();

	}
}
