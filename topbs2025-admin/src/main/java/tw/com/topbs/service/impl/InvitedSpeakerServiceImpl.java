package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.InvitedSpeakerConvert;
import tw.com.topbs.mapper.InvitedSpeakerMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddInvitedSpeakerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutInvitedSpeakerDTO;
import tw.com.topbs.pojo.entity.InvitedSpeaker;
import tw.com.topbs.service.InvitedSpeakerService;

/**
 * <p>
 * 受邀請的講者，可能是講者，可能是座長 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-04-23
 */
@Service
@RequiredArgsConstructor
public class InvitedSpeakerServiceImpl extends ServiceImpl<InvitedSpeakerMapper, InvitedSpeaker>
		implements InvitedSpeakerService {

	private final InvitedSpeakerConvert invitedSpeakerConvert;

	@Override
	public InvitedSpeaker getInvitedSpeaker(Long id) {
		InvitedSpeaker invitedSpeaker = baseMapper.selectById(id);
		return invitedSpeaker;
	}

	@Override
	public List<InvitedSpeaker> getAllInvitedSpeaker() {
		List<InvitedSpeaker> invitedSpeakerList = baseMapper.selectList(null);
		return invitedSpeakerList;
	}

	@Override
	public IPage<InvitedSpeaker> getInvitedSpeakerPage(Page<InvitedSpeaker> page) {
		Page<InvitedSpeaker> invitedSpeakerPage = baseMapper.selectPage(page, null);
		return invitedSpeakerPage;
	}

	@Override
	public void addInvitedSpeaker(AddInvitedSpeakerDTO addInvitedSpeakerDTO) {
		InvitedSpeaker invitedSpeaker = invitedSpeakerConvert.addDTOToEntity(addInvitedSpeakerDTO);
		baseMapper.insert(invitedSpeaker);

	}

	@Override
	public void updateInvitedSpeaker(PutInvitedSpeakerDTO putInvitedSpeakerDTO) {
		InvitedSpeaker invitedSpeaker = invitedSpeakerConvert.putDTOToEntity(putInvitedSpeakerDTO);
		baseMapper.updateById(invitedSpeaker);
	}

	@Override
	public void deleteInvitedSpeaker(Long id) {
		baseMapper.deleteById(id);
	}

}
