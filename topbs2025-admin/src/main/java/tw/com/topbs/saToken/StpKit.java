package tw.com.topbs.saToken;

import org.springframework.stereotype.Component;

import cn.dev33.satoken.session.SaSession;
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
	 * 審稿委員 會話對象，管理 paper_reviewer 表所有帳號的登入、權限註冊
	 */
	public static final StpLogic PAPER_REVIEWER = new StpLogic("paper-reviewer") {
		// 重写 StpLogic 类下的 `splicingKeyTokenName` 函数，
		// 返回一个与 `StpUtil` 不同的token名称, 防止冲突
		@Override
		public String splicingKeyTokenName() {
			return super.splicingKeyTokenName() + "-paper-reviewer";
		}

	};

	/**
	 * 方便Satoken 透過註解調用要執行哪個 帳號體系認證
	 */
	public static final String PAPER_REVIEWER_TYPE = "paper-reviewer";

	/**
	 * XX 会话对象，（项目中有多少套账号表，就声明几个 StpLogic 会话对象）
	 */
	public static final StpLogic XXX = new StpLogic("xxx");

	/**
	 * 根據登入類型獲取對應的 StpLogic 實例
	 * 
	 * @param loginType 登入類型
	 * @return 對應的 StpLogic 實例
	 */
	public static StpLogic getStpLogic(String loginType) {
		
		if (PAPER_REVIEWER_TYPE.equals(loginType)) {
			return PAPER_REVIEWER;
		} else if (MEMBER_TYPE.equals(loginType)) {
			return MEMBER;
		} else if ("admin".equals(loginType)) {
			return ADMIN;
		} else {
			return DEFAULT;
		}
	}

	/**
	 * 根據登入類型獲取對應的 SaSession
	 * 
	 * @param loginType 登入類型
	 * @return 對應的 SaSession
	 */
	public static SaSession getSessionByLoginType(String loginType) {
		return getStpLogic(loginType).getSession();
	}

}
