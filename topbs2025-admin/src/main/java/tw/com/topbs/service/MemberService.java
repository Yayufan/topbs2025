package tw.com.topbs.service;

import java.io.UnsupportedEncodingException;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.dev33.satoken.stp.SaTokenInfo;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import tw.com.topbs.exception.RegistrationInfoException;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.GroupRegistrationDTO;
import tw.com.topbs.pojo.DTO.MemberLoginInfo;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;

public interface MemberService extends IService<Member> {

	Member getMember(Long memberId);

	List<Member> getMemberList();

	IPage<Member> getMemberPage(Page<Member> page);

	Long getMemberCount();

	Integer getMemberOrderCount(String status);

	IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, String status, String queryText);

	/**
	 * 獲取尚未付款的會員列表
	 * 
	 * @param page
	 * @param queryText
	 * @return
	 */
	IPage<MemberVO> getUnpaidMemberList(Page<Member> page, String queryText);
	
	/**
	 * 新增會員，同時當作註冊功能使用，會自行產生會費訂單，且回傳tokenInfo
	 * 
	 * @param addMemberDTO
	 * @return
	 * @throws Exception
	 */
	SaTokenInfo addMember(AddMemberDTO addMemberDTO) throws RegistrationInfoException;

	/**
	 * 新增會員，後台管理者使用
	 * 
	 * @param addMemberForAdminDTO
	 */
	void addMemberForAdmin(AddMemberForAdminDTO addMemberForAdminDTO);

	/**
	 * 新增團體報名會員，會自行產生會費訂單給主報名者
	 * 
	 * @param groupRegistrationDTO
	 * @throws InterruptedException
	 */
	void addGroupMember(GroupRegistrationDTO groupRegistrationDTO);

	void updateMember(PutMemberDTO putMemberDTO);
	
	/**
	 * 給予memberId 快速去修改 orders 註冊費Item , 改為已付款
	 * 
	 * @param memberId
	 */
	void approveUnpaidMember(Long memberId);

	void deleteMember(Long memberId);

	void deleteMemberList(List<Long> memberIds);

	Member getMemberInfo();
	
	/**
	 * 下載所有會員列表, 其中要包含他們當前的付款狀態
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException;

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
	void forgetPassword(String email) throws MessagingException;

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
	IPage<MemberTagVO> getAllMemberTagVOByQuery(Page<Member> page, String queryText, Integer status);

	/**
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的Members
	 * 如果沒有傳任何tag則是寄給所有Member
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void sendEmailToMembers(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

}
