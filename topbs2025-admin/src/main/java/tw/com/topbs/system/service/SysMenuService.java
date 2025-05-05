package tw.com.topbs.system.service;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.system.pojo.entity.SysMenu;

/**
 * <p>
 * 菜單表-最底層細部權限也存在這張表,包含路由、路由組件、路由參數... 組裝動態路由返回給前端 服务类
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
public interface SysMenuService extends IService<SysMenu> {

	
	
}
