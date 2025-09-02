package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.ScheduleEmailRecordConvert;
import tw.com.topbs.mapper.ScheduleEmailRecordMapper;
import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.service.ScheduleEmailRecordService;

/**
 * <p>
 * 排程寄信任務的收信者,及信件內容 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
@Service
@RequiredArgsConstructor
public class ScheduleEmailRecordServiceImpl extends ServiceImpl<ScheduleEmailRecordMapper, ScheduleEmailRecord>
		implements ScheduleEmailRecordService {

	private final ScheduleEmailRecordConvert scheduleEmailRecordConvert;

	@Override
	public ScheduleEmailRecord getScheduleEmailRecord(Long id) {
		ScheduleEmailRecord scheduleEmailRecord = baseMapper.selectById(id);
		return scheduleEmailRecord;
	}

	@Override
	public List<ScheduleEmailRecord> getScheduleEmailRecordList() {
		List<ScheduleEmailRecord> scheduleEmailRecordList = baseMapper.selectList(null);
		return scheduleEmailRecordList;
	}

	@Override
	public IPage<ScheduleEmailRecord> getScheduleEmailRecordPage(Page<ScheduleEmailRecord> page) {
		Page<ScheduleEmailRecord> scheduleEmailRecordPage = baseMapper.selectPage(page, null);
		return scheduleEmailRecordPage;
	}

	@Override
	public Long addScheduleEmailRecord(ScheduleEmailRecord scheduleEmailRecord) {
		baseMapper.insert(scheduleEmailRecord);
		return scheduleEmailRecord.getScheduleEmailRecordId();
	}

	@Override
	public void deleteScheduleEmailRecord(Long scheduleEmailRecordId) {
		baseMapper.deleteById(scheduleEmailRecordId);
	}

}
