package tw.com.topbs.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.dev33.satoken.stp.SaTokenInfo;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import tw.com.topbs.pojo.DTO.AddGroupMemberDTO;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.MemberLoginInfo;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Setting;

public interface MemberService extends IService<Member> {

	Member getMember(Long memberId);

	List<Member> getMemberList();

	IPage<Member> getMemberPage(Page<Member> page);

	Long getMemberCount();

	/**
	 * 根據email查詢是否有這個會員
	 * 
	 * @param email
	 * @return
	 */
	Member getMemberByEmail(String email);
	
	Integer getMemberOrderCount(List<Orders> orderList);

	IPage<MemberOrderVO> getMemberOrderVO(IPage<Orders> orderPage, Integer status, String queryText);

	/**
	 * 獲取尚未付款的會員列表
	 * 
	 * @param orderList
	 * @param queryText
	 * @return
	 */
	IPage<MemberVO> getUnpaidMemberPage(Page<Member> page, List<Orders> orderList, String queryText);

	/**
	 * 根據設定中的註冊時間進行校驗<br>
	 * 並計算該會員的註冊費用
	 * 
	 * @param setting      配置設定
	 * @param addMemberDTO
	 * @return
	 */
	BigDecimal validateAndCalculateFee(Setting setting, AddMemberDTO addMemberDTO);


	BigDecimal validateAndCalculateFeeForGroup(Setting setting,List<AddGroupMemberDTO> addGroupMemberDTOList);
	
	/**
	 * 拿到當前團體標籤的index
	 * 
	 * @param groupSize 一組的數量(人數)
	 * @return
	 */
	int getMemberGroupIndex(int groupSize);

	/**
	 * 校驗email是否註冊過<br>
	 * 沒有則,新增會員,並返回會員資料
	 * 
	 * @param addMemberDTO
	 * @return
	 */
	Member addMember(AddMemberDTO addMemberDTO);

	/**
	 * 後台管理者新增<br>
	 * 校驗email是否註冊過<br>
	 * 沒有則,新增會員,並返回會員資料
	 * 
	 * @param addMemberForAdminDTO
	 * @return
	 */
	Member addMemberForAdmin(AddMemberForAdminDTO addMemberForAdminDTO);

	Member addMemberByRoleAndGroup(String groupCode,String groupRole,AddGroupMemberDTO addGroupMemberDTO);
	
	void updateMember(PutMemberDTO putMemberDTO);

	void deleteMember(Long memberId);

	void deleteMemberList(List<Long> memberIds);

	Member getMemberInfo();

	/**
	 * 下載所有會員列表, 其中要包含他們當前的付款狀態
	 * 
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 * 
	 */
	void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException, IOException;

	/**
	 * 會員登入，用於註冊後立馬登入使用
	 * 
	 * @param memberLoginInfo
	 * @return
	 */
	SaTokenInfo login(Member member);

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
	 * 立刻寄送
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的Members
	 * 如果沒有傳任何tag則是寄給所有Member
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void sendEmailToMembers(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 排程寄送
	 * 前端給予tag列表，以及信件內容，透過tag列表去查詢要寄信的Members
	 * 如果沒有傳任何tag則是寄給所有Member
	 * 
	 * @param tagIdList
	 * @param sendEmailDTO
	 */
	void scheduleEmailToMembers(List<Long> tagIdList, SendEmailDTO sendEmailDTO);

	/**
	 * 更換信件內MergeTag
	 * 
	 * @param content 信件內容
	 * @param member  替換資料源
	 * @return
	 */
	String replaceMemberMergeTag(String content, Member member);

}
