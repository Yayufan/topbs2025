package tw.com.topbs.system.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.system.pojo.DTO.LoginInfo;
import tw.com.topbs.system.pojo.VO.SysUserVO;
import tw.com.topbs.system.service.SysUserService;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 用戶表 - 存取系統用戶個人信息 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
@Tag(name = "後台管理者API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/sys-user")
public class SysUserController {

	private final SysUserService sysUserService;

	/**
	 * 根據ID查詢User 後台管理者
	 * 
	 * @param id
	 * @return User
	 */
	@Operation(summary = "根據ID查詢後台管理者及其權限")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@GetMapping("{id}")
	public R<SysUserVO> getUser(@PathVariable("id") Long id) {
		SysUserVO sysUserVO = sysUserService.selectSysUser(id);

		return R.ok(sysUserVO);
	}

	/**
	 * 查詢所有User 後台管理者
	 * 
	 * @return
	 */
	@Operation(summary = "查詢所有後台管理者及其權限")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@GetMapping
	public R<List<SysUserVO>> getAllUser() {
		List<SysUserVO> userList = sysUserService.selectAllSysUser();
		return R.ok(userList);
	}

//	/**
//	 * 新增User 後台管理者 待更新
//	 * 
//	 * @param user
//	 * @return
//	 */
//	@Operation(summary = "新增後台管理者")
//	@Parameters({
//			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
//	@SaCheckRole("super-admin")
//	@PostMapping
//	public R<Void> saveUser(@RequestBody UserDTO userDTO) {
//		// 待更新
//		sysUserService.insertSysUser();
//		return R.ok();
//	}

	/**
	 * 根據實體類User 更新後台管理者 待更新
	 * 
	 * @param user
	 * @return
	 */
//	@Operation(summary = "更新後台管理者資訊")
//	@Parameters({
//			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
//	@SaCheckRole("super-admin")
//	@PutMapping
//	public R<Void> updateUser(@RequestBody User user) {
//		// 待更新
//		sysUserService.updateSysUser();
//		return R.ok();
//	}

	/**
	 * 根據ID移除User 後台管理者 如果返回的User對象為null則返回R.fail
	 * 
	 * @param id
	 * @return
	 */
	@Operation(summary = "根據ID刪除後台管理者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER), })
	@SaCheckRole("super-admin")
	@DeleteMapping("{id}")
	public R<Void> removeUser(@PathVariable Long id) {
		sysUserService.deleteSysUser(id);
		return R.ok();
	}

	/**
	 * 
	 * @param loginInfo
	 * @return
	 */
	@Operation(summary = "管理者登入")
	@PostMapping("login")
	public R<SaTokenInfo> Login(@RequestBody @Validated LoginInfo loginInfo) {

		// 驗證登入資料
		sysUserService.login(loginInfo);

		// 登入後才能獲得token信息，獲取token
		SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

		// 返回token
		return R.ok(tokenInfo);

	}

	/**
	 * 管理者登出方法
	 * 
	 * @return
	 */
	@SaCheckLogin
	@Operation(summary = "管理者登出")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER), })
	@PostMapping("logout")
	public R<Void> LogOut() {

		// 驗證登入資料
		sysUserService.logout();

		// 返回token
		return R.ok();

	}

	/**
	 * 
	 * @param loginInfo
	 * @return
	 */
	@Operation(summary = "獲取緩存內的管理者資訊")
	@SaCheckLogin
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER), })
	@GetMapping("getUserInfo")
	public R<SysUserVO> GetUserInfo() {

		// 驗證登入資料
		SysUserVO userInfo = sysUserService.getUserInfo();

		// 返回token
		return R.ok(userInfo);

	}

}
