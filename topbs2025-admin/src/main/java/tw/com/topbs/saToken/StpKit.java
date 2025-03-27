package tw.com.topbs.saToken;

import org.springframework.stereotype.Component;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系 用於處理多帳號體系認證問題
 * 這邊有個細節，在多帳號體系下使用，他是需要讓Spring 進行初始化的，所以需要加上@Component 
 * 將 Class 放入容器中
 * 
 */

@Component
public class StpKit {

	/**
	 * 默认原生会话对象
	 */
	public static final StpLogic DEFAULT = StpUtil.stpLogic;

	/**
	 * Admin 会话对象，管理 Admin 表所有账号的登录、权限认证
	 */
	public static final StpLogic ADMIN = new StpLogic("admin");

	/**
	 * Member 会话对象，管理 Member 表所有账号的登录、权限认证
	 */
	public static final StpLogic MEMBER = new StpLogic("member") {
		// 重写 StpLogic 类下的 `splicingKeyTokenName` 函数，
		// 返回一个与 `StpUtil` 不同的token名称, 防止冲突
		@Override
		public String splicingKeyTokenName() {
			return super.splicingKeyTokenName() + "-member";
		}

	};
	/**
	 * 方便Satoken 透過註解調用要執行哪個 帳號體系認證
	 */
	public static final String MEMBER_TYPE = "member";
	

	/**
	 * XX 会话对象，（项目中有多少套账号表，就声明几个 StpLogic 会话对象）
	 */
	public static final StpLogic XXX = new StpLogic("xxx");

}
