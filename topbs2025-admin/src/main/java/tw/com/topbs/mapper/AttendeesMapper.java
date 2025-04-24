package tw.com.topbs.mapper;

import tw.com.topbs.pojo.entity.Attendees;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
public interface AttendeesMapper extends BaseMapper<Attendees> {

}
