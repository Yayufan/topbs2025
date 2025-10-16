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
import tw.com.topbs.enums.RegistrationPhaseEnum;
import tw.com.topbs.enums.GroupRegistrationEnum;
import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.exception.RegistrationClosedException;
import tw.com.topbs.pojo.DTO.AddGroupMemberDTO;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.EmailBodyContent;
import tw.com.topbs.pojo.DTO.GroupRegistrationDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
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
import tw.com.topbs.utils.CountryUtil;

@Component
@RequiredArgsConstructor
public class MemberRegistrationManager {

	@Value("${project.name}")
	private String PROJECT_NAME ;
	
	@Value("${project.banner-url}")
	private String BANNER_PHOTO_URL ;
	
	@Value("${project.group-size}")
	private int GROUP_SIZE ;

	private RegistrationFeeConfig registrationFeeConfig;
	
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

		// 1.先判斷是否處於註冊時間內
		if(!settingService.isRegistrationOpen()) {
			throw new RegistrationClosedException("The registration time has ended");
		}
		
		// 2.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();
		
		// 3.透過Country 拿到國籍 , 只分國內國外,	
		String country = CountryUtil.getTaiwanOrForeign(addMemberDTO.getCountry());
		
		// 4.拿到身分
		MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(addMemberDTO.getCategory());
		
		// 5.透過階段、國籍、身分，得到金額
		BigDecimal membershipFee = registrationFeeConfig.getFee(registrationPhaseEnum.getValue(),country, memberCategoryEnum.getConfigKey());
		
		// 6.新增會員
		Member member = memberService.addMember(addMemberDTO);

		// 7.創建註冊費訂單
		ordersService.createRegistrationOrder(membershipFee, member);

		// 8.創建註冊成功通知信件內容
		EmailBodyContent registrationSuccessContent = notificationService.generateRegistrationSuccessContent(member,
				BANNER_PHOTO_URL);

		// 9.異步寄送信件
		asyncService.sendCommonEmail(member.getEmail(), PROJECT_NAME + " Registration Successful",
				registrationSuccessContent.getHtmlContent(), registrationSuccessContent.getPlainTextContent());

		// 10.獲取當下Member群體的Index,用於後續標籤分組
		int memberGroupIndex = memberService.getMemberGroupIndex(GROUP_SIZE);

		// 11.會員標籤分組
		// 呼叫 Manager 拿到 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
		// 關聯 Member 與 Tag
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 12.返回token , 讓用戶於註冊後登入
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

	@Transactional
	public void addGroupMember(GroupRegistrationDTO groupRegistrationDTO) {

		BigDecimal totalFee = BigDecimal.ZERO;
		Member member;
		
		// 1.先判斷是否處於團體註冊時間內,這邊還沒改好
		if(!settingService.isRegistrationOpen()) {
			throw new RegistrationClosedException("The group registration time has ended");
		}
		
		// 2.拿到配置設定,知道處於哪個註冊階段
		RegistrationPhaseEnum registrationPhaseEnum = settingService.getRegistrationPhaseEnum();
		
		// 3.在外部直接產生團體的代號
		String groupCode = UUID.randomUUID().toString();

		// 4.提取團體報名的所有人，方便後續調用
		List<AddGroupMemberDTO> groupMembers = groupRegistrationDTO.getGroupMembers();
		
		// 5.計算所有成員的費用總和
		for (AddGroupMemberDTO addGroupMemberDTO : groupMembers) {
			// 5-1.透過Country 拿到國籍 , 只分國內國外,		
			String country = CountryUtil.getTaiwanOrForeign(addGroupMemberDTO.getCountry());
			
			// 5-2.拿到身分
			MemberCategoryEnum memberCategoryEnum = MemberCategoryEnum.fromValue(addGroupMemberDTO.getCategory());
			
			// 5-3.透過階段、國籍、身分，得到金額
			BigDecimal membershipFee = registrationFeeConfig.getFee(registrationPhaseEnum.getValue(),country, memberCategoryEnum.getConfigKey());
		
			// 5-4.添加回總額
			totalFee.add(membershipFee);
		}
		

		// 6.折扣後的金額總額(9折)，這邊根據團體報名折扣給優惠
		BigDecimal discountedTotalFee = totalFee.multiply(BigDecimal.valueOf(0.9));

		// 7.團體報名有複數會員,遍歷進行新增
		for (int i = 0; i < groupMembers.size(); i++) {

			// 7-1獲取當前團體報名對象
			AddGroupMemberDTO addGroupMemberDTO = groupMembers.get(i);

			// 7-2針對團體中的Role,做不同的新增/產生訂單操作
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

			// 7-3獲取當下Member群體的Index,用於後續標籤分組
			int memberGroupIndex = memberService.getMemberGroupIndex(GROUP_SIZE);

			// 7-4會員標籤分組
			// 拿到 Tag（不存在則新增Tag）
			Tag groupTag = tagService.getOrCreateMemberGroupTag(memberGroupIndex);
			// 關聯 Member 與 Tag
			memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

			// 7-5產生系統團體報名通知信
			EmailBodyContent groupRegistrationSuccessContent = notificationService
					.generateGroupRegistrationSuccessContent(member, BANNER_PHOTO_URL);

			// 7-6寄信個別通知會員，團體報名成功
			asyncService.sendCommonEmail(member.getEmail(), PROJECT_NAME+" GROUP Registration Successful",
					groupRegistrationSuccessContent.getHtmlContent(),
					groupRegistrationSuccessContent.getPlainTextContent());

		}

	}

}
