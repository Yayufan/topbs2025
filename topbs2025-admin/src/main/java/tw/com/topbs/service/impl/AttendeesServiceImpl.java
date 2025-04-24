package tw.com.topbs.service.impl;

import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.service.AttendeesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
@Service
public class AttendeesServiceImpl extends ServiceImpl<AttendeesMapper, Attendees> implements AttendeesService {

}
