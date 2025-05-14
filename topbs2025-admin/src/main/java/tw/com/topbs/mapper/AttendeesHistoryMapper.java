package tw.com.topbs.mapper;

import tw.com.topbs.pojo.entity.AttendeesHistory;

import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 往年與會者名單 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
public interface AttendeesHistoryMapper extends BaseMapper<AttendeesHistory> {

	/**
	 * 快速清空整張表,因為舊資料通常是別人匯進來的,所以沒什麼關係
	 * 
	 */
	@Update("TRUNCATE TABLE attendees_history")
	void cleanAllData();
	
}
