package tw.com.topbs.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.system.pojo.DTO.LoginInfo;
import tw.com.topbs.system.pojo.VO.SysUserVO;
import tw.com.topbs.system.pojo.entity.SysUser;

/**
 * <p>
 * 用戶表 - 存取系統用戶個人信息 服务类
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
public interface SysUserService extends IService<SysUser> {

	/**
	 * 根據ID查詢系統管理者的資料、角色、權限
	 * 
	 * @param id 管理者ID
	 * @return 系統管理者詳細資訊
	 */
	SysUserVO selectSysUser(Long id);
	
	/**
	 * 查詢所有系統管理者的資料、角色、權限
	 * @return 系統管理者列表詳細資訊
	 */
	List<SysUserVO> selectAllSysUser();
	
	Void insertSysUser();
	
	Void updateSysUser();
	
	/**
	 * 根據ID查詢刪除系統管理者
	 * 
	 * @param id 管理者ID
	 * 
	 */
	Void deleteSysUser(Long id);
	
	
	
	/**
	 * 系統管理者登入方法,返回token、角色、權限
	 * 前端傳來一個由email 和 password組裝的 LoginInfo對象 先判斷帳號密碼取得SysUser的ID
	 * 將組裝好的sysUserInfo 放到緩存中
	 * 
	 * @param LoginInfo
	 * @return SysUserVO
	 */
	SysUserVO login(LoginInfo loginInfo);
	
	
	/**
	 * 登出方法
	 * 
	 * 
	 */
	void logout();
	
	
	SysUserVO getUserInfo();
	
}
