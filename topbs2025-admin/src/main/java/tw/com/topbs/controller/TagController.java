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
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.AddAssociatedToTagDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutTagDTO;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.TagService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 標籤表,用於對Member進行分組 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "標籤API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/tag")
public class TagController {

	private final TagService tagService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一標籤")
	public R<Tag> getTag(@PathVariable("id") Long tagId) {
		Tag tag = tagService.getTag(tagId);
		return R.ok(tag);
	}

	@GetMapping
	@Operation(summary = "查詢所有標籤")
	public R<List<Tag>> getAllTag() {
		List<Tag> tagList = tagService.getTagList();
		return R.ok(tagList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢所有標籤(分頁)")
	public R<IPage<Tag>> getAllTag(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String tagType) {
		Page<Tag> pageInfo = new Page<>(page, size);
		IPage<Tag> tagList = tagService.getTagPageByType(pageInfo, tagType);
		return R.ok(tagList);
	}

	@Operation(summary = "新增標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@PostMapping
	public R<Void> saveTag(@RequestBody AddTagDTO insertTagDTO) {
		tagService.insertTag(insertTagDTO);
		return R.ok();

	}

	@Operation(summary = "更新標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@PutMapping
	public R<Void> updateTag(@RequestBody PutTagDTO updateTagDTO) {
		tagService.updateTag(updateTagDTO);
		return R.ok();

	}

	@Operation(summary = "刪除標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@DeleteMapping("{id}")
	public R<Void> deleteTag(@PathVariable("id") Long tagId) {
		tagService.deleteTag(tagId);
		return R.ok();

	}

	/** -------------------- 以下為為標籤 及 持有者操作 ----------------------------- */

	@Operation(summary = "根據標籤ID 返回 關聯ID列表(associatedIdList)", description = "memberTag => 取得memberIdSet <br> paperTag => 取得paperIdSet")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@GetMapping("associated/{tagId}")
	public R<List<Long>> getAssociatedIdListByTagId(@PathVariable("tagId") Long tagId) {
		List<Long> associatedIds = tagService.getAssociatedIdsByTagId(tagId);
		return R.ok(associatedIds);

	}

	@Operation(summary = "為標籤 新增/更新/刪除 複數關聯")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("associated")
	public R<Void> assignAssociatedToTag(@Validated @RequestBody AddAssociatedToTagDTO addAssociatedToTagDTO) {
		tagService.assignAssociatedToTag(addAssociatedToTagDTO.getTargetAssociatedIdList(),
				addAssociatedToTagDTO.getTagId());
		return R.ok();

	}

	@GetMapping("holder/{id}")
	@Operation(summary = "查詢單一標籤的持有人數")
	public R<Long> countHoldersByTagId(@PathVariable("id") Long tagId) {
		long holders = tagService.countHoldersByTagId(tagId);
		return R.ok(holders);
	}

	@GetMapping("holder")
	@Operation(summary = "查詢某些標籤的持有人數，(標籤需為同一類型)")
	public R<Long> countHoldersByTagId(@RequestParam("tagIds") List<Long> tagIds) {
		long holders = tagService.countHoldersByTagIds(tagIds);
		return R.ok(holders);
	}

}
