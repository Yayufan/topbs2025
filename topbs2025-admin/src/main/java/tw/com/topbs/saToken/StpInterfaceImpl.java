package tw.com.topbs.saToken;

import java.util.List;

import org.springframework.stereotype.Component;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import tw.com.topbs.system.pojo.VO.SysUserVO;

@Component
public class StpInterfaceImpl implements StpInterface {
	/**
	 * 返回一个账号所拥有的权限码集合 只要有使用SaToken權限驗證的功能,他都會在這個攔截器先跑一次 所以最好將這個用戶的權限集合進行緩存 緩存失效時,
	 * 
	 */
	@Override
	public List<String> getPermissionList(Object loginId, String loginType) {
		
		System.out.println("訪問權限列表的ID:" + loginId);
		System.out.println("訪問權限列表的類型為:" + loginType);

		// 根據帳號體系，獲取相對應的權限校驗邏輯
		StpLogic stpLogic = StpKit.getStpLogic(loginType);
		SaSession session = stpLogic.getSession();

		// 定義當前使用者的資料
		SysUserVO sysUserVO = null;
		// 定義當前使用者的權限列表
		List<String> permissionList = null;

		// 如果從緩存獲取的使用者資料不為空,則為變量從新賦值
		if (session.get("userInfo") != null) {
			// 獲取當前使用者的資料
			sysUserVO = (SysUserVO) session.get("userInfo");
			// 獲取當前使用的權限列表
			permissionList = sysUserVO.getPermissionList();
		}

		// 打印權限
		System.out.println("攔截器攔截判斷,取出權限的列表: " + permissionList);

		System.out.println("最終返回權限集合");

		return permissionList;
		
		
	}

	/**
	 * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
	 */
	@Override
	public List<String> getRoleList(Object loginId, String loginType) {
		
		
		System.out.println("訪問角色列表的ID:" + loginId);
		System.out.println("訪問角色列表的類型為:" + loginType);
		
		StpLogic stpLogic = StpKit.getStpLogic(loginType);
		SaSession session = stpLogic.getSession();
		
//		SaSession session = StpKit.PAPER_REVIEWER.getSession();
		// 獲得預設帳號體系的使用者的session
//		SaSession session = StpUtil.getSession();

//		System.out.println("session keys : " + session.keys());
//		System.out.println("session token : " + session.getToken());
//		System.out.println("session ID : " + session.getId());
		
		// 定義當前使用者的資料
		SysUserVO sysUserVO = null;
		// 定義當前使用者的權限
		List<String> roleList = null;

		// 如果從緩存獲取的使用者資料不為空,則為變量從新賦值
		if (session.get("userInfo") != null) {
			System.out.println("進入取出: userInfo");
			
			// 獲取當前使用者的資料
			sysUserVO = (SysUserVO) session.get("userInfo");
			// 獲取當前使用者角色列表
			roleList = sysUserVO.getRoleList();
		}
		
		// 打印權限
		System.out.println("攔截器攔截判斷,取出角色列表: " + roleList); // 取值

		System.out.println("最終返回角色集合");

		return roleList;
		
	}

}
