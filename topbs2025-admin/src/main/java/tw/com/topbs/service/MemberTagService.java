package tw.com.topbs.service;

import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.entity.MemberTag;

/**
 * <p>
 * member表 和 tag表的關聯表 服务类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
public interface MemberTagService extends IService<MemberTag> {

	/**
	 * 根據 memberId 查詢與之有關的所有MemberTag關聯
	 * 
	 * @param memberId
	 * @return
	 */
	List<MemberTag> getMemberTagByMemberId(Long memberId);

	/**
	 * 根據 tagId 查詢與之有關的所有MemberTag關聯
	 * 
	 * @param tagId
	 * @return
	 */
	List<MemberTag> getMemberTagByTagId(Long tagId);

	/**
	 * 根據 memberId 集合， 查詢與之有關的所有MemberTag關聯
	 * 
	 * @param memberIds
	 * @return
	 */
	List<MemberTag> getMemberTagByMemberIds(Collection<Long> memberIds);

	/**
	 * 根據 tagIds 集合， 查詢與之有關的所有MemberTag關聯
	 * 
	 * @param tagIds
	 * @return
	 */
	List<MemberTag> getMemberTagByTagIds(Collection<Long> tagIds);

	/**
	 * 為一個tag和member新增關聯
	 * 
	 * @param memberTag
	 */
	void addMemberTag(MemberTag memberTag);
	
	/**
	 * 透過 memberId 和 tagId 建立關聯
	 * 
	 * @param memberId 會員ID
	 * @param tagId 標籤ID
	 */
	void addMemberTag(Long memberId, Long tagId);

	/**
	 * 根據標籤 ID 刪除多個會員 關聯
	 * 
	 * @param tagId
	 * @param membersToRemove
	 */
	void removeMembersFromTag(Long tagId, Collection<Long> membersToRemove);

	/**
	 * 根據會員 ID 刪除多個標籤關聯
	 * 
	 * @param memberId
	 * @param tagsToRemove
	 */
	void removeTagsFromMember(Long memberId, Collection<Long> tagsToRemove);

}
