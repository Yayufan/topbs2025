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
import tw.com.topbs.convert.SettingConvert;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddSettingDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutSettingDTO;
import tw.com.topbs.pojo.VO.SettingVO;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.utils.R;

@Tag(name = "設定API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/setting")
public class SettingController {

	private final SettingService settingService;
	private final SettingConvert settingConvert;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一設定")
	@SaCheckRole("super-admin")
	public R<Setting> getSetting(@PathVariable("id") Long settingId) {
		Setting setting = settingService.getSetting(settingId);
		return R.ok(setting);
	}

	@GetMapping
	@Operation(summary = "查詢全部設定")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<SettingVO>> getUserList() {
		List<Setting> settingList = settingService.getSettingList();
		List<SettingVO> settingVOList = settingConvert.entityListToVOList(settingList);
		return R.ok(settingVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部設定(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<Setting>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Setting> pageable = new Page<Setting>(page, size);
		IPage<Setting> settingPage = settingService.getSettingPage(pageable);
		return R.ok(settingPage);
	}

	@PostMapping
	@Operation(summary = "新增單一設定")
	@SaCheckRole("super-admin")
	public R<Setting> saveSetting(@RequestBody @Valid AddSettingDTO addSettingDTO) {
		settingService.addSetting(addSettingDTO);
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "修改設定")
	public R<Setting> updateSetting(@RequestBody @Valid PutSettingDTO putSettingDTO) {
		settingService.updateSetting(putSettingDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除設定")
	public R<Setting> deleteSetting(@PathVariable("id") Long settingId) {
		settingService.deleteSetting(settingId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除設定")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeleteSetting(@RequestBody List<Long> ids) {
		settingService.deleteSettingList(ids);
		return R.ok();

	}

}
