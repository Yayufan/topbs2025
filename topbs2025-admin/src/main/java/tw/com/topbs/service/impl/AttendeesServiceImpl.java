package tw.com.topbs.service.impl;

import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.service.AttendeesService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

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

	@Override
	public void addAttendees() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addAfterPayment() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AttendeesVO getAttendees(Long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AttendeesVO> getAllAttendees() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPage<AttendeesVO> getAllAttendees(Page<Attendees> page) {
		// TODO Auto-generated method stub
		return null;
	}

}
