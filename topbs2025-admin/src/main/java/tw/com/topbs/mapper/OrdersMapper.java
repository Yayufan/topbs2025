package tw.com.topbs.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.pojo.entity.Orders;

/**
 * <p>
 * 訂單表 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
public interface OrdersMapper extends BaseMapper<Orders> {

	@Select("""
		    SELECT * 
		    FROM orders 
		    WHERE is_deleted = 0 
		      AND (items_summary = #{registration} OR items_summary = #{groupRegistration})
		    """)
		List<Orders> selectOrders(
		    @Param("registration") String registration,
		    @Param("groupRegistration") String groupRegistration
		);
	
}
