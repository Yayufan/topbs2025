package tw.com.topbs.service;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import jakarta.validation.Valid;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddInvitedSpeakerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutInvitedSpeakerDTO;
import tw.com.topbs.pojo.entity.InvitedSpeaker;

/**
 * <p>
 * 受邀請的講者，可能是講者，可能是座長 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-04-23
 */
@Validated
public interface InvitedSpeakerService extends IService<InvitedSpeaker> {

	InvitedSpeaker getInvitedSpeaker(Long id);

	List<InvitedSpeaker> getAllInvitedSpeaker();
	
	IPage<InvitedSpeaker> getInvitedSpeakerPage(Page<InvitedSpeaker> page);

	void addInvitedSpeaker(MultipartFile file, @Valid AddInvitedSpeakerDTO addInvitedSpeakerDTO);

	void updateInvitedSpeaker(MultipartFile file, @Valid PutInvitedSpeakerDTO putInvitedSpeakerDTO);

	void deleteInvitedSpeaker(Long id);

}
