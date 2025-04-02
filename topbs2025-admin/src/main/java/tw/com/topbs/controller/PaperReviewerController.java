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

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.utils.R;

@Tag(name = "審稿委員API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paperReviewer")
public class PaperReviewerController {

	private final PaperReviewerService paperReviewerService;
	private final PaperReviewerConvert paperReviewerConvert;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一審稿委員")
	public R<PaperReviewerVO> getPaperReviewer(@PathVariable("id") Long paperReviewerId) {
		PaperReviewerVO paperReviewerVO = paperReviewerService.getPaperReviewer(paperReviewerId);
		return R.ok(paperReviewerVO);
	}

	@GetMapping
	@Operation(summary = "查詢全部審稿委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<PaperReviewerVO>> getPaperReviewerList() {
		List<PaperReviewerVO> paperReviewerVOList = paperReviewerService.getPaperReviewerList();
		return R.ok(paperReviewerVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部審稿委員(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<PaperReviewerVO>> getPaperReviewerPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<PaperReviewer> pageable = new Page<PaperReviewer>(page, size);
		IPage<PaperReviewerVO> paperReviewerVOPage = paperReviewerService.getPaperReviewerPage(pageable);
		return R.ok(paperReviewerVOPage);
	}

	@PostMapping
	@Operation(summary = "新增單一審稿委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<PaperReviewer> savePaperReviewer(@RequestBody @Valid AddPaperReviewerDTO addPaperReviewerDTO) {
		paperReviewerService.addPaperReviewer(addPaperReviewerDTO);
		return R.ok();
	}
	

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "修改審稿委員")
	@SaCheckRole("super-admin")
	public R<PaperReviewer> updatePaperReviewer(@RequestBody @Valid PutPaperReviewerDTO putPaperReviewerDTO) {
		paperReviewerService.updatePaperReviewer(putPaperReviewerDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除審稿委員")
	public R<PaperReviewer> deletePaperReviewer(@PathVariable("id") Long paperReviewerId) {
		paperReviewerService.deletePaperReviewer(paperReviewerId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除審稿委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeletePaperReviewer(@RequestBody List<Long> ids) {
		paperReviewerService.deletePaperReviewerList(ids);
		return R.ok();

	}
}
