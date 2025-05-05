package tw.com.topbs.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.system.pojo.entity.SysMenu;

/**
 * <p>
 * 菜單表-最底層細部權限也存在這張表,包含路由、路由組件、路由參數... 組裝動態路由返回給前端 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2024-05-10
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

}
