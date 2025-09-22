package tw.com.topbs.manager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.SaTokenInfo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.GroupRegistrationEnum;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.pojo.DTO.AddGroupMemberDTO;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.GroupRegistrationDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
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

@Component
@RequiredArgsConstructor
public class MemberRegistrationManager {

	private int groupSize = 200;
	private String BANNER_PHOTO_URL = "https://iopbs2025.org.tw/_nuxt/banner.CL2lyu9P.png";

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

	/**
	 * 註冊功能,新增會員,產生「付費」訂單
	 * 
	 * @param addMemberDTO
	 * @return
	 */
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
				BANNER_PHOTO_URL);

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

	/**
	 * 後台新增會員功能,產生「免費」訂單
	 * 
	 * @param addMemberForAdminDTO
	 */
	@Transactional
	public void addMemberForAdmin(AddMemberForAdminDTO addMemberForAdminDTO) {

		// 1.判斷Email是否被註冊，如果沒有新增會員
		Member member = memberService.addMemberForAdmin(addMemberForAdminDTO);

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

	@Transactional
	public void addGroupMember(GroupRegistrationDTO groupRegistrationDTO) {

		Member member;

		// 1.拿到配置設定
		Setting setting = settingService.getSetting();

		// 2.在外部直接產生團體的代號
		String groupCode = UUID.randomUUID().toString();

		// 3.提取團體報名的所有人，方便後續調用
		List<AddGroupMemberDTO> groupMembers = groupRegistrationDTO.getGroupMembers();

		// 4.所有成員的費用總和
		BigDecimal totalFee = memberService.validateAndCalculateFeeForGroup(setting, groupMembers);

		// 5.折扣後的金額總額(9折)
		BigDecimal discountedTotalFee = totalFee.multiply(BigDecimal.valueOf(0.9));

		// 6.團體報名有複數會員,遍歷進行新增
		for (int i = 0; i < groupMembers.size(); i++) {

			// 6-1獲取當前團體報名對象
			AddGroupMemberDTO addGroupMemberDTO = groupMembers.get(i);

			// 6-2針對團體中的Role,做不同的新增/產生訂單操作
			if (i == 0) {
				// 新增會員，Master
				member = memberService.addMemberByRoleAndGroup(groupCode, GroupRegistrationEnum.MASTER.getValue(),
						addGroupMemberDTO);

				// master 負責負所有錢，新增有價格的團體註冊費訂單，狀態為「未付款」 
				ordersService.createGroupRegistrationOrder(discountedTotalFee, member);

				// 其餘則為團體成員
			} else {
				// 新增會員，Slave
				member = memberService.addMemberByRoleAndGroup(groupCode, GroupRegistrationEnum.SLAVE.getValue(),
						addGroupMemberDTO);

				// slave 不用付錢，直接新增0元團體註冊訂單，狀態為「未付款」 
				ordersService.createFreeGroupRegistrationOrder(member);

			}

			// 6-3獲取當下Member群體的Index,用於後續標籤分組
			int memberGroupIndex = memberService.getMemberGroupIndex(groupSize);

			// 6-4會員標籤分組
			// 拿到 Tag（不存在則新增Tag）
			Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
			// 關聯 Member 與 Tag
			memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

			// 6-5產生系統團體報名通知信
			EmailBodyContent groupRegistrationSuccessContent = notificationService
					.generateGroupRegistrationSuccessContent(member, BANNER_PHOTO_URL);

			// 6-6寄信個別通知會員，團體報名成功
			asyncService.sendCommonEmail(member.getEmail(), "2025 TOPBS & IOPBS GROUP Registration Successful",
					groupRegistrationSuccessContent.getHtmlContent(),
					groupRegistrationSuccessContent.getPlainTextContent());

		}

	}

}
