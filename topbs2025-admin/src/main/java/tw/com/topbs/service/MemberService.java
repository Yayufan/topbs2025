package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.dev33.satoken.stp.SaTokenInfo;
import jakarta.mail.MessagingException;
import tw.com.topbs.pojo.DTO.MemberLoginInfo;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;

public interface MemberService extends IService<Member> {

	Member getMember(Long memberId);

	List<Member> getMemberList();

	IPage<Member> getMemberPage(Page<Member> page);
	
	Long getMemberCount();
	
	Integer getMemberOrderCount(String status);
	
	IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page,String status,String queryText);

	/**
	 * 新增會員，同時當作註冊功能使用，會自行產生會費訂單，且回傳tokenInfo
	 * 
	 * @param addMemberDTO
	 * @return
	 */
	SaTokenInfo addMember(AddMemberDTO addMemberDTO);

	void updateMember(PutMemberDTO putMemberDTO);

	void deleteMember(Long memberId);

	void deleteMemberList(List<Long> memberIds);
	
	Member getMemberInfo();
	
	/**
	 * 會員登入
	 * 
	 * @param memberLoginInfo
	 * @return
	 */
	SaTokenInfo login(MemberLoginInfo memberLoginInfo);

	/**
	 * 會員登出
	 */
	void logout();

	/**
	 * 寄信找回密碼
	 * 
	 * @param email
	 * @throws MessagingException
	 */
	Member forgetPassword(String email) throws MessagingException;
	
	/**
	 * 為用戶新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param memberId
	 */
	void assignTagToMember(List<Long> targetTagIdList, Long memberId);

	
	/**
	 * 根據memberId，獲取會員資料及持有的tag集合
	 * 
	 * @param memberId
	 * @return
	 */
	MemberTagVO getMemberTagVOByMember(Long memberId);
	
	
	/**
	 * 獲取所有 會員資料及持有的tag集合(分頁)
	 * 
	 * @param page
	 * @return
	 */
	IPage<MemberTagVO> getAllMemberTagVO(Page<Member> page);
	
	/**
	 * 根據搜尋條件 獲取會員資料及持有的tag集合(分頁)
	 * 
	 * @param page
	 * @param queryText
	 * @param status
	 * @return
	 */
	IPage<MemberTagVO> getAllMemberTagVOByQuery(Page<Member> page,String queryText,String status);
	
	

}
