package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
public interface AttendeesService extends IService<Attendees> {

	AttendeesVO getAttendees(Long id);
	
	List<AttendeesVO> getAllAttendees();
	
	IPage<AttendeesVO> getAllAttendees(Page<Attendees> page);
	
	void addAfterPayment();
	
	void addAttendees();
	
	
	
}
