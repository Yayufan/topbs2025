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

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.R;

@Tag(name = "稿件API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paper")
public class PaperController {

	private final PaperService paperService;
	private final PaperConvert paperConvert;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一稿件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Paper> getPaper(@PathVariable("id") Long paperId) {
		Paper paper = paperService.getPaper(paperId);
		return R.ok(paper);
	}

	@GetMapping
	@Operation(summary = "查詢全部稿件")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<List<PaperVO>> getUserList() {
		List<Paper> paperList = paperService.getPaperList();
		List<PaperVO> paperVOList = paperConvert.entityListToVOList(paperList);
		return R.ok(paperVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部稿件(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<IPage<Paper>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Paper> pageable = new Page<Paper>(page, size);
		IPage<Paper> paperPage = paperService.getPaperPage(pageable);
		return R.ok(paperPage);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "新增單一稿件", description = """
			使用 FormData 上傳稿件資料，files、data 為 key，files 包含多個檔案。
			
			Json 用Blob進行包裝：

			const jsonData = JSON.stringify(addPaperDTO)

			formData.append('data', new Blob([jsonData], { type: "application/json" }))

			formData.append("files", file); // 多次 append，同一個 key

			""")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Paper> savePaper(
			@RequestPart("files") @Schema(type = "string", format = "binary", description = "可以傳輸多個文件") MultipartFile[] files,
			@RequestPart("data") @Valid AddPaperDTO addPaperDTO) {

		paperService.addPaper(files, addPaperDTO);
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Operation(summary = "修改稿件")
	public R<Paper> updatePaper(@RequestBody @Valid PutPaperDTO putPaperDTO) {
		paperService.updatePaper(putPaperDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除稿件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Paper> deletePaper(@PathVariable("id") Long paperId) {
		paperService.deletePaper(paperId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除稿件")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> batchDeletePaper(@RequestBody List<Long> ids) {
		paperService.deletePaperList(ids);
		return R.ok();

	}
}
