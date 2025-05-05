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

	/**
	 * 根據ID，獲取invitedSpeaker
	 * 
	 * @param id
	 * @return
	 */
	InvitedSpeaker getInvitedSpeaker(Long id);

	/**
	 * 獲取所有invitedSpeaker
	 * 
	 * @return
	 */
	List<InvitedSpeaker> getAllInvitedSpeaker();

	/**
	 * 獲取所有invitedSpeaker(分頁)
	 * 
	 * @param page 分頁資訊
	 * @return
	 */
	IPage<InvitedSpeaker> getInvitedSpeakerPage(Page<InvitedSpeaker> page);
	
	/**
	 * 根據查詢字串，獲取所有invitedSpeaker(分頁)
	 * 
	 * @param page 分頁資訊
	 * @param queryText 模糊查詢字串
	 * @return
	 */
	IPage<InvitedSpeaker> getInvitedSpeakerPage(Page<InvitedSpeaker> page,String queryText);

	/**
	 * 新增 invitedSpeaker (受邀講者)
	 * 
	 * @param file                 圖檔
	 * @param addInvitedSpeakerDTO 講者資訊
	 */
	void addInvitedSpeaker(MultipartFile file, @Valid AddInvitedSpeakerDTO addInvitedSpeakerDTO);

	/**
	 * 更新 invitedSpeaker (受邀講者)
	 * 
	 * @param file                 圖檔
	 * @param putInvitedSpeakerDTO 講者資訊
	 */
	void updateInvitedSpeaker(MultipartFile file, @Valid PutInvitedSpeakerDTO putInvitedSpeakerDTO);

	/**
	 * 根據ID，刪除invitedSpeaker
	 * 
	 * @param id
	 */
	void deleteInvitedSpeaker(Long id);

	/**
	 * 根據ID列表，刪除invitedSpeakers
	 * 
	 * @param ids
	 */
	void deleteInvitedSpeakerList(List<Long> ids);

}
