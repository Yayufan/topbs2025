package tw.com.topbs.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.entity.CheckinRecord;

/**
 * <p>
 * 簽到退紀錄 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
public interface CheckinRecordMapper extends BaseMapper<CheckinRecord> {
	// 因為是For Excel 匯出使用，所以按照與會者排序
		@Select("SELECT * FROM checkin_record WHERE is_deleted = 0 ORDER BY attendees_id,action_time")
		List<CheckinRecord> selectCheckinRecords();

		/**
		 * 查詢 實到 人數
		 * 
		 * @return
		 */
		@Select("SELECT COUNT(DISTINCT attendees_id) FROM checkin_record" + " WHERE is_deleted = 0 AND action_type = 1")
		Integer countCheckedIn();


		/**
		 * 查詢 尚在會場 和 已離場 人數
		 * 
		 * @return
		 */
		PresenceStatsBO selectPresenceStats();

}
