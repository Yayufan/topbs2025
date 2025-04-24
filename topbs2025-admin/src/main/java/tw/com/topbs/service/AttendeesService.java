package tw.com.topbs.service;

import tw.com.topbs.pojo.entity.Attendees;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
public interface AttendeesService extends IService<Attendees> {

	
	void addAttendees();
	
	
	
}
