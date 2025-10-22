package tw.com.topbs.manager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.config.RegistrationFeeConfig;
import tw.com.topbs.constant.I18nMessageKey;
import tw.com.topbs.context.ProjectModeContext;
import tw.com.topbs.enums.GroupRegistrationEnum;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.enums.RegistrationPhaseEnum;
import tw.com.topbs.exception.RegistrationClosedException;
import tw.com.topbs.helper.MessageHelper;
import tw.com.topbs.pojo.DTO.AddGroupMemberDTO;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.GroupRegistrationDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.InvitedSpeakerService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;
import tw.com.topbs.utils.CountryUtil;

@Component
@RequiredArgsConstructor
public class MemberRegistrationManager {

	@Value("${project.name}")
	private String PROJECT_NAME;

	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL;

	@Value("${project.group-size}")
	private int GROUP_SIZE;

	private final RegistrationFeeConfig registrationFeeConfig;

	private final ProjectModeContext projectModeContext;

	private final MessageHelper messageHelper;
	private final MemberService memberService;
	private final OrdersService ordersService;
	private final AttendeesService attendeesService;
	private final TagService tagService;
	private final MemberTagService memberTagService;
	private final AttendeesTagService attendeesTagService;
	private final SettingService settingService;
	private final InvitedSpeakerService invitedSpeakerService;

	/**
	 * 註冊功能,新增會員,產生「付費」訂單
	 * 
	 * @param addMemberDTO
	 * @return
	 */
	@Transactional
	public SaTokenInfo addMember(AddMemberDTO addMemberDTO) {

		// 1.先判斷是否處於註冊時間內
		if (!settingService.isRegistrationOpen()) {
			throw new RegistrationClosedException(messageHelper.get(I18nMessageKey.Registration.CLOSED));
		}

		// 2.新增會員
		Member member = memberService.addMember(addMemberDTO);

		// 3.以當前模式策略,執行註冊流程 (計算金額=>產生訂單=>產生通知信並寄出)
		projectModeContext.getStrategy().handleRegistration(member);

		// 4.獲取當下Member群體的Index,用於後續標籤分組
		int memberGroupIndex = memberService.getMemberGroupIndex(GROUP_SIZE);

		// 5.會員標籤分組
		// 呼叫 Manager 拿到 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
		// 關聯 Member 與 Tag
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 6.返回token , 讓用戶於註冊後登入
		return memberService.login(member);
	}

	@Transactional
	public void addGroupMember(GroupRegistrationDTO groupRegistrationDTO) {

		// 1.先判斷是否處於團體註冊時間內,這邊還沒改好
		if (!settingService.isRegistrationOpen()) {
			throw new RegistrationClosedException(messageHelper.get(I18nMessageKey.Registration.CLOSED));
		}

		// 2.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();

		// 3.在外部直接產生團體的代號
		String groupCode = UUID.randomUUID().toString();

		// 4.提取團體報名的所有人，方便後續調用
		List<AddGroupMemberDTO> groupMembers = groupRegistrationDTO.getGroupMembers();

		// 5.計算所有成員的費用總和，折扣後的金額總額(9折
		BigDecimal discountedTotalFee = groupMembers.stream()
				.map(m -> registrationFeeConfig.getFee(registrationPhaseEnum.getValue(),
						CountryUtil.getTaiwanOrForeign(m.getCountry()),
						MemberCategoryEnum.fromValue(m.getCategory()).getConfigKey()))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.multiply(BigDecimal.valueOf(0.9)); // 團體折扣 9折 , 這邊有需要記得改

		// 6.團體報名有複數會員,遍歷進行新增
		for (int i = 0; i < groupMembers.size(); i++) {

			// 6-1獲取當前團體報名對象
			AddGroupMemberDTO addGroupMemberDTO = groupMembers.get(i);
			boolean isMaster = i == 0;

			// 6-2新增會員
			Member member = memberService.addMemberByRoleAndGroup(groupCode,
					isMaster ? GroupRegistrationEnum.MASTER.getValue() : GroupRegistrationEnum.SLAVE.getValue(),
					addGroupMemberDTO);

			// 6-3以當前模式,去執行團體報名的策略
			projectModeContext.getStrategy().handleGroupRegistration(member, isMaster, discountedTotalFee);

			// 6-4獲取當下Member群體的Index,用於後續標籤分組
			int memberGroupIndex = memberService.getMemberGroupIndex(GROUP_SIZE);

			// 6-5會員標籤分組
			// 拿到 Tag（不存在則新增Tag）
			Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
			// 關聯 Member 與 Tag
			memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		}

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
		int memberGroupIndex = memberService.getMemberGroupIndex(GROUP_SIZE);

		// 4.會員標籤分組
		// 拿到 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
		// 關聯 Member 與 Tag
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 5.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 6.獲取當下 Attendees 群體的Index,用於後續標籤分組
		int attendeesGroupIndex = attendeesService.getAttendeesGroupIndex(GROUP_SIZE);

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
