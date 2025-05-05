package tw.com.topbs.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.system.convert.SysMenuConvert;
import tw.com.topbs.system.convert.SysUserConvert;
import tw.com.topbs.system.mapper.SysMenuMapper;
import tw.com.topbs.system.mapper.SysRoleMapper;
import tw.com.topbs.system.mapper.SysRoleMenuMapper;
import tw.com.topbs.system.mapper.SysUserMapper;
import tw.com.topbs.system.mapper.SysUserRoleMapper;
import tw.com.topbs.system.pojo.BO.RouteBO;
import tw.com.topbs.system.pojo.DTO.LoginInfo;
import tw.com.topbs.system.pojo.VO.SysUserVO;
import tw.com.topbs.system.pojo.entity.SysMenu;
import tw.com.topbs.system.pojo.entity.SysRole;
import tw.com.topbs.system.pojo.entity.SysRoleMenu;
import tw.com.topbs.system.pojo.entity.SysUser;
import tw.com.topbs.system.pojo.entity.SysUserRole;
import tw.com.topbs.system.service.SysUserService;

/**
 * <p>
 * 用戶表 - 存取系統用戶個人信息 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */

@RequiredArgsConstructor
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

	private final SysUserConvert sysUserConvert;
	private final SysMenuConvert sysMenuConvert;
	private final SysUserMapper baseMapper;
	private final SysUserRoleMapper sysUserRoleMapper;
	private final SysRoleMapper sysRoleMapper;
	private final SysRoleMenuMapper sysRoleMenuMapper;
	private final SysMenuMapper sysMenuMapper;

	@Override
	public SysUserVO selectSysUser(Long id) {

		// 獲取用戶資料
		SysUser sysUser = baseMapper.selectById(id);

		// 調用Service層私有方法,來獲取需要組裝的SysUserVO
		SysUserVO sysUserVO = getSysUserVO(sysUser);

		// 返回組裝過的SysUserVO
		return sysUserVO;
	}

	@Override
	public List<SysUserVO> selectAllSysUser() {
		// 創建空List,用於蒐集SysUserVO
		List<SysUserVO> sysUserVOList = new ArrayList<>();
		// 獲取所有SysUser用戶資料
		List<SysUser> sysUserList = baseMapper.selectList(null);

		// 使用增強for循環遍歷
		for (SysUser sysUser : sysUserList) {

			// 調用Service層私有方法,來獲取需要組裝的SysUserVO
			SysUserVO sysUserVO = getSysUserVO(sysUser);

			// 組裝完畢放入List中
			sysUserVOList.add(sysUserVO);
		}

		// 返回List<SysUserVO>
		return sysUserVOList;
	}

	@Override
	public Void insertSysUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void updateSysUser() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void deleteSysUser(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SysUserVO login(LoginInfo loginInfo) {

		// 判斷帳號密碼
		LambdaQueryWrapper<SysUser> loginQueryWrapper = new LambdaQueryWrapper<>();
		loginQueryWrapper.eq(SysUser::getEmail, loginInfo.getEmail()).eq(SysUser::getPassword, loginInfo.getPassword());

		// 獲取用戶資料
		SysUser sysUser = baseMapper.selectOne(loginQueryWrapper);

		// 調用Service層私有方法,來獲取需要組裝的SysUserVO
		SysUserVO sysUserVO = getSysUserVO(sysUser);

		// 透過userId做登入
		StpUtil.login(sysUserVO.getSysUserId());

		// 登入後才能取得session
		SaSession session = StpUtil.getSession();

		// 登入後才能獲得token信息
		SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

		// 將登入信息進行組裝
		sysUserVO.setSaTokenInfo(tokenInfo);

		// 設定session
		session.set("userInfo", sysUserVO);

		// 返回susUserVO對象給前端
		return sysUserVO;
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub

		// 当前会话注销登录
		StpUtil.logout();

	}

	// 以下為Service層,各個function同樣會使用的私有方法
	private SysUserVO getSysUserVO(SysUser sysUser) {

		// VO對象,填充用戶資訊
		SysUserVO sysUserVO = sysUserConvert.entityToVO(sysUser);

		// 創建Lmabda選擇器，從用戶-角色表中,根據用戶Id找到他所擁有的角色ID
		LambdaQueryWrapper<SysUserRole> sysUserRoleWrapper = new LambdaQueryWrapper<>();
		sysUserRoleWrapper.eq(SysUserRole::getSysUserId, sysUser.getSysUserId());

		// 透過選擇器找到用戶對應的角色ID,透過stream拿到純數字的數組
		List<SysUserRole> sysUserRoleList = sysUserRoleMapper.selectList(sysUserRoleWrapper);
		List<Long> roleIdList = sysUserRoleList.stream().map(SysUserRole::getSysRoleId).collect(Collectors.toList());

		// 創建Lmabda選擇器，從角色表中查詢包含角色Id列表中的所有角色資訊
		LambdaQueryWrapper<SysRole> sysRoleWrapper = new LambdaQueryWrapper<>();
		sysRoleWrapper.in(SysRole::getSysRoleId, roleIdList);

		// 透過選擇器找到擁有的角色列表,透過stream拿到純文字的角色權限標示符數組
		List<SysRole> sysRoleList = sysRoleMapper.selectList(sysRoleWrapper);
		List<String> roleList = sysRoleList.stream().map(SysRole::getRoleKey).collect(Collectors.toList());

		// VO對象,填充用戶所擁有的角色
		sysUserVO.setRoleList(roleList);

		// 創建Lmabda選擇器，從角色-菜單表中查詢包含角色ID列表中的所有菜單資訊
		LambdaQueryWrapper<SysRoleMenu> sysRoleMenuWrapper = new LambdaQueryWrapper<>();
		sysRoleMenuWrapper.in(!roleIdList.isEmpty(), SysRoleMenu::getSysRoleId, roleIdList);

		// 透過選擇器找到角色擁有的菜單列表,透過stream拿到純文字的菜單ID List
		List<SysRoleMenu> sysRoleMenuList = sysRoleMenuMapper.selectList(sysRoleMenuWrapper);
		List<Long> menuIdList = sysRoleMenuList.stream().map(SysRoleMenu::getSysMenuId).collect(Collectors.toList());

		// 創建Lmabda選擇器，從菜單表中查詢包含菜單ID列表中的所有菜單資訊
		LambdaQueryWrapper<SysMenu> sysMenuWrapper = new LambdaQueryWrapper<>();
		sysMenuWrapper.in(!menuIdList.isEmpty(), SysMenu::getSysMenuId, menuIdList);

		// 獲取角色們所擁有菜單權限
		List<SysMenu> sysMenuList = sysMenuMapper.selectList(sysMenuWrapper);

		// 獲取權限列表
		List<String> permissionList = sysMenuList.stream()
				.filter(menu -> menu.getPermission() != null && !menu.getPermission().isEmpty())
				.map(SysMenu::getPermission).collect(Collectors.toList());

		// VO對象,填充用戶所擁有的權限
		sysUserVO.setPermissionList(permissionList);

		// 使用Menu資料組裝user的路由
		ArrayList<RouteBO> allRouteList = new ArrayList<>();

		// 遍歷Menu先取得全部的路由
		for (SysMenu sysMenu : sysMenuList) {
			// 當Menu的類型不為F , function級別時,代表他是M(Menu菜單), 或者C(Core核心頁面)
			if (!sysMenu.getMenuType().equals("F")) {

				// sysMenu轉換成BO對象, 用於組裝返回前端的路由對象
				RouteBO route = sysMenuConvert.entityToBO(sysMenu);

				// 將每個路由對象添加到List, 成為一個路由數組ArrayList<RouteBO>
				allRouteList.add(route);
			}
		}

		// 使用递归组装父子路由
		List<RouteBO> routeVO = new ArrayList<>();
		for (RouteBO route : allRouteList) {
			// 當這個路由的parentId為0 那麼他是最頂層的路由
			if (route.getParentId() == 0) {

				assembleRoutes(route, allRouteList);
				// 裝進返回給前端的路由中
				routeVO.add(route);
			}
		}

		// 將組裝後的父子路由放路sysUserVO
		sysUserVO.setRouteList(routeVO);

		return sysUserVO;
	}

	/**
	 * 遞歸function 用來重新組裝成父子路由的
	 * 
	 * @param parent 當前路由
	 * @param routes 所有路由List
	 */
	private void assembleRoutes(RouteBO parent, List<RouteBO> routes) {
		for (RouteBO route : routes) {
			if (route.getParentId() == parent.getMenuId()) {
				// 遞歸執行assembleRoutes , 確保子路由都已经被正确地组装到了当前路由对象中
				assembleRoutes(route, routes);
				parent.addChild(route);
			}
		}
	}

	@Override
	public SysUserVO getUserInfo() {

		// 登入後才能取得session
		SaSession session = StpUtil.getSession();
		// 獲取當前使用者的資料
		SysUserVO sysUserVO = (SysUserVO) session.get("userInfo");

		return sysUserVO;
	}

}
