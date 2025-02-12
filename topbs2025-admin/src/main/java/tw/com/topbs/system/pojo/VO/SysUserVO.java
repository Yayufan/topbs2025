package tw.com.topbs.system.pojo.VO;

import java.util.List;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Data;
import tw.com.topbs.system.pojo.BO.RouteBO;


/**
 * 傳給前端的系統使用者對象
 * 
 */
@Data
public class SysUserVO {

	private Long userId;
	private String realName;
	private String nickName;
	private String email;
	private String phone;
	private String status;
	
	//token信息
	private SaTokenInfo saTokenInfo;
	
	//權限集合
	private List<String> roleList;
	private List<String> permissionList;
	//路由集合
	private List<RouteBO> routeList;
	
	
	
	
}
