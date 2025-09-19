package tw.com.topbs.manager;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.InvitedSpeakerService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.NotificationService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;

/**
 * 管理會員 和 訂單的需求,<br>
 * 以及成為與會者流程組裝
 */
@Component
@RequiredArgsConstructor
public class MemberOrderManager {

	private int groupSize = 200;

	private final MemberService memberService;
	private final OrdersService ordersService;
	private final AttendeesService attendeesService;
	private final TagService tagService;
	private final MemberTagService memberTagService;
	private final AttendeesTagService attendeesTagService;
	private final SettingService settingService;
	private final InvitedSpeakerService invitedSpeakerService;
	private final NotificationService notificationService;
	private final AsyncService asyncService;

	// --------------------------- 查詢相關 ---------------------------------------

	/**
	 * 獲得訂單狀態的會員人數
	 * 
	 * @param status
	 * @return
	 */
	public Integer getMemberOrderCount(Integer status) {

		// 1.查找符合訂單狀態的訂單
		List<Orders> registrationOrderList = ordersService.getRegistrationOrderListByStatus(status);

		// 2.返回當前訂單狀態的會員總人數
		return memberService.getMemberOrderCount(registrationOrderList);

	}

	/**
	 * 獲得會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param status
	 * @param queryText
	 * @return
	 */
	public IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, Integer status, String queryText) {
		// 1.根據分頁 和 訂單狀態, 拿到分頁對象
		Page<Orders> orderPage = ordersService.getRegistrationOrderPageByStatus(page, status);

		// 2.再把訂單分頁 和 會員的查詢條件放入,拿到VO對象並返回
		IPage<MemberOrderVO> memberOrderVO = memberService.getMemberOrderVO(orderPage, status, queryText);
		return memberOrderVO;
	}

	/**
	 * 適用於不使用金流,人工審核<br>
	 * 獲得本國未付款的 會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param queryText
	 * @return
	 */
	public IPage<MemberVO> getUnpaidMemberPage(Page<Member> page, String queryText) {

		// 1.本國籍的快速搜索 (外國團體報名不在此限)
		List<Orders> unpaidRegistrationOrderList = ordersService.getUnpaidRegistrationOrderList();

		// 2.拿到本國籍,未付款的分頁對象
		IPage<MemberVO> unpaidMemberPage = memberService.getUnpaidMemberPage(page, unpaidRegistrationOrderList,
				queryText);
		return unpaidMemberPage;
	}

	// --------------------------- 新增相關 ---------------------------------------

	@Transactional
	public SaTokenInfo addMember(AddMemberDTO addMemberDTO) {

		// 1.拿到配置設定
		Setting setting = settingService.getSetting();

		// 2.校驗註冊時間,並計算會員應繳註冊費 ,後續準備優化 ,他可能違反SRP
		BigDecimal membershipFee = memberService.validateAndCalculateFee(setting, addMemberDTO);

		// 3.新增會員
		Member member = memberService.addMember(addMemberDTO);

		// 4.創建註冊費訂單
		ordersService.createRegistrationOrder(membershipFee, member);

		// 5.創建註冊成功通知信件內容
		EmailBodyContent registrationSuccessContent = notificationService.generateRegistrationSuccessContent(member,
				"https://iopbs2025.org.tw/_nuxt/banner.CL2lyu9P.png");

		// 6.異步寄送信件
		asyncService.sendCommonEmail(member.getEmail(), "2025 TOPBS & IOPBS  Registration Successful",
				registrationSuccessContent.getHtmlContent(), registrationSuccessContent.getPlainTextContent());

		// 7.獲取當下Member群體的Index,用於後續標籤分組
		int memberGroupIndex = memberService.getMemberGroupIndex(groupSize);

		// 8.會員標籤分組
		// 呼叫 Manager 拿到 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
		// 關聯 Member 與 Tag
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 9.返回token , 讓用戶於註冊後登入
		return memberService.login(member);
	}

	@Transactional
	public void addMemberForAdmin(AddMemberForAdminDTO addMemberForAdminDTO) {

		// 1.判斷Email是否被註冊，如果沒有新增會員
		Member member = memberService.addMemberForAdminM(addMemberForAdminDTO);

		// 2.新增「免費」的訂單,並標註 「已付款」
		ordersService.createFreeRegistrationOrder(member);

		// 3.獲取當下Member群體的Index,用於後續標籤分組
		int memberGroupIndex = memberService.getMemberGroupIndex(groupSize);

		// 4.會員標籤分組
		// 拿到 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
		// 關聯 Member 與 Tag
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 5.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 6.獲取當下 Attendees 群體的Index,用於後續標籤分組
		int attendeesGroupIndex = attendeesService.getAttendeesGroupIndex(groupSize);

		// 7.與會者標籤分組
		// 拿到 Tag（不存在則新增Tag）
		Tag attendeesGroupTag = tagService.getOrCreateAttendeesGroupTag(attendeesGroupIndex);
		// 關聯 Attendees 與 Tag
		attendeesTagService.addAttendeesTag(attendees.getAttendeesId(), attendeesGroupTag.getTagId());

		// 8.如果是講者身分,則新增到invited-speaker, 這個也再考慮, 可能違反SRP
		if (MemberCategoryEnum.SPEAKER.getValue().equals(member.getCategory())) {
			invitedSpeakerService.addInviredSpeaker(member);
		}

	}

}
