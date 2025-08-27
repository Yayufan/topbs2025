package tw.com.topbs.service.impl;

import tw.com.topbs.pojo.entity.ScheduleEmailRecord;
import tw.com.topbs.mapper.ScheduleEmailRecordMapper;
import tw.com.topbs.service.ScheduleEmailRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 排程寄信任務的收信者,及信件內容 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-08-27
 */
@Service
public class ScheduleEmailRecordServiceImpl extends ServiceImpl<ScheduleEmailRecordMapper, ScheduleEmailRecord> implements ScheduleEmailRecordService {

}
