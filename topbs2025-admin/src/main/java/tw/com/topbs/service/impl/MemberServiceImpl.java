package tw.com.topbs.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.MemberConvert;
import tw.com.topbs.convert.TagConvert;
import tw.com.topbs.enums.OrderStatusEnum;
import tw.com.topbs.exception.AccountPasswordWrongException;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.exception.ForgetPasswordException;
import tw.com.topbs.exception.RegisteredAlreadyExistsException;
import tw.com.topbs.exception.RegistrationClosedException;
import tw.com.topbs.exception.RegistrationInfoException;
import tw.com.topbs.manager.OrdersManager;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.BO.MemberExcelRaw;
import tw.com.topbs.pojo.DTO.AddGroupMemberDTO;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.GroupRegistrationDTO;
import tw.com.topbs.pojo.DTO.MemberLoginInfo;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddAttendeesDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.pojo.excelPojo.MemberExcel;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.OrdersItemService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.SettingService;
import tw.com.topbs.service.TagService;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private static final String MEMBER_CACHE_INFO_KEY = "memberInfo";
	private static final String ITEMS_SUMMARY_REGISTRATION = "Registration Fee";
	private static final String GROUP_ITEMS_SUMMARY_REGISTRATION = "Group Registration Fee";

	private final MemberConvert memberConvert;
	private final TagConvert tagConvert;

	private final MemberTagService memberTagService;
	private final OrdersService ordersService;
	private final OrdersManager ordersManager;
	private final OrdersItemService ordersItemService;
	private final AttendeesService attendeesService;
	private final TagService tagService;
	private final SettingService settingService;
	private final AsyncService asyncService;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public Member getMember(Long memberId) {
		Member member = baseMapper.selectById(memberId);
		return member;
	}

	@Override
	public List<Member> getMemberList() {
		List<Member> memberList = baseMapper.selectList(null);
		return memberList;
	}

	@Override
	public IPage<Member> getMemberPage(Page<Member> page) {
		Page<Member> memberPage = baseMapper.selectPage(page, null);
		return memberPage;
	}

	@Override
	public Long getMemberCount() {
		Long memberCount = baseMapper.selectCount(null);
		return memberCount;
	}

	@Override
	public Integer getMemberOrderCount(Integer status) {
		// 查找註冊費訂單(註冊費/團體註冊費) ,符合繳費狀態的訂單 
		List<Orders> registrationOrdersByStatus = ordersManager.getRegistrationOrderListByStatus(status);
		// 從訂單中抽出memberId,變成List
		List<Long> memberIdList = registrationOrdersByStatus.stream()
				.map(Orders::getMemberId)
				.collect(Collectors.toList());

		// 返回代表符合繳費狀態的註冊費訂單的會員人數
		return memberIdList.size();
	}

	@Override
	public IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, Integer status, String queryText) {
		// 1. 根據註冊費訂單繳費狀態,得到訂單分頁對象
		Page<Orders> ordersPage = ordersManager.getRegistrationOrderPageByStatus(page, status);

		// 2. 從訂單分頁對象提取memberId 成為List
		List<Long> memberIdList = ordersPage.getRecords()
				.stream()
				.map(Orders::getMemberId)
				.collect(Collectors.toList());

		// 
		if (CollectionUtils.isEmpty(memberIdList)) {
			return new Page<>(); // 沒有符合的訂單，返回空分頁對象
		}

		// 用 memberIdList 查询 member 表，要先符合MemberId表且如果queryText不為空，則模糊查詢姓名、Email和帳號末五碼
		LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		memberQueryWrapper.in(Member::getMemberId, memberIdList)
				.and(StringUtils.isNotBlank(queryText),
						wrapper -> wrapper.like(Member::getFirstName, queryText)
								.or()
								.like(Member::getLastName, queryText)
								.or()
								.like(Member::getEmail, queryText)
								.or()
								.like(Member::getRemitAccountLast5, queryText));

		List<Member> memberList = baseMapper.selectList(memberQueryWrapper);

		// 建立兩個映射 (Map) 以便後續資料整合
		// ordersMap：利用 groupingBy 將 Orders 按 memberId 分組，結果為 Map<Long, List<Orders>>。
		/**
		 * 方法： Collectors.groupingBy() 是一個很常用的收集器（collector）方法，它將集合中的元素根據某個條件（這裡是
		 * Orders::getMemberId）進行分組，並返回一個 Map，其鍵是分組的依據（memberId），值是分組後的集合（這裡是
		 * List<Orders>）。 用途：這個 Map 的目的是將訂單（Orders）資料按 memberId 進行分組，使得每個會員的訂單可以集中在一起。
		 * 使用原因：每個會員可能有多個訂單，因此需要將多個訂單放在同一個 List 中，並且按 memberId 分組。這是使用 groupingBy
		 * 的原因，它非常適合這種需求。
		 */
		Map<Long, List<Orders>> ordersMap = ordersPage.getRecords()
				.stream()
				.collect(Collectors.groupingBy(Orders::getMemberId));

		// memberMap：使用 .toMap() 以 memberId 為鍵，Member 物件本身為值，快速查找會員資料。
		/**
		 * 方法：Collectors.toMap() 用於將集合中的每個元素轉換成一個鍵值對，並生成一個 Map。這裡的鍵是
		 * Member::getMemberId，而值是 Member 物件本身。 用途：這個 Map 的目的是將每個 Member 物件與其 memberId
		 * 進行映射，並保證可以通過 memberId 快速找到對應的 Member 資料。 使用原因：在查詢 Member 資料後，我們需要快速查找某個
		 * memberId 對應的 Member 資料，使用 toMap 能夠直接將 Member 和 memberId
		 * 做一一對應，從而可以迅速獲取會員的詳細資訊。
		 */
		Map<Long, Member> memberMap = memberList.stream().collect(Collectors.toMap(Member::getMemberId, m -> m));

		// 整合資料並轉為 MemberOrderVO
		List<MemberOrderVO> voList = memberIdList.stream().map(memberId -> {
			// 查詢每個會員的詳細資料
			Member member = memberMap.get(memberId);
			// 如果會員資料為 null，直接返回 null（代表此 memberId 在 Member 表中找不到）。
			if (member == null) {
				return null;
			}

			// MapStruct直接轉換大部分屬性至VO
			MemberOrderVO vo = memberConvert.entityToMemberOrderVO(member);

			// 確保即使某會員沒有訂單，也不會出錯。
			vo.setOrdersList(ordersMap.getOrDefault(memberId, new ArrayList<>()));

			return vo;
			// 過濾掉 null 的 VO； 匯總成 List。
		}).filter(Objects::nonNull).collect(Collectors.toList());

		/**
		 * 組裝分頁對象返回， new Page<>(...)：建立一個新的分頁對象，並設定： ordersPage.getCurrent()：當前頁碼。
		 * ordersPage.getSize()：每頁大小。 ordersPage.getTotal()：總記錄數。
		 * .setRecords(voList)：將組裝完成的 MemberOrderVO 資料設置到結果頁中。
		 */

		IPage<MemberOrderVO> resultPage = new Page<>(ordersPage.getCurrent(), ordersPage.getSize(),
				ordersPage.getTotal());
		resultPage.setRecords(voList);

		return resultPage;

	}

	@Override
	public IPage<MemberVO> getUnpaidMemberList(Page<Member> page, String queryText) {

		// 先從訂單表內查詢，尚未付款，且ItemSummary為註冊費的訂單列表
		// 是For Taiwan本國籍的快速搜索 (外國團體報名不在此限)
		List<Orders> ordersList = ordersManager.getUnpaidRegistrationOrderList();

		// 從訂單表中提取出會員ID 列表
		Set<Long> memberIdSet = ordersList.stream().map(orders -> orders.getMemberId()).collect(Collectors.toSet());

		// 如果會員ID不為Null 以及 集合內元素不為空
		if (memberIdSet != null && !memberIdSet.isEmpty()) {

			// 查找國家為Taiwan, 有 '註冊費' 這張訂單且處於未繳費的 memberIdList，且如果有額外查詢資料 or 進行模糊查詢
			LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
			memberWrapper.eq(Member::getCountry, "Taiwan")
					.in(Member::getMemberId, memberIdSet)
					.and(StringUtils.isNotBlank(queryText), wrapper -> {
						wrapper.like(Member::getRemitAccountLast5, queryText)
								.or()
								.like(Member::getChineseName, queryText)
								.or()
								.like(Member::getIdCard, queryText);
					});

			Page<Member> memberPage = baseMapper.selectPage(page, memberWrapper);

			// 對數據做轉換，轉成vo對象，設定vo的status(付款狀態) 為 0
			List<MemberVO> voList = memberPage.getRecords().stream().map(member -> {
				MemberVO vo = memberConvert.entityToVO(member);
				vo.setStatus(OrderStatusEnum.UNPAID.getValue());
				return vo;
			}).collect(Collectors.toList());

			Page<MemberVO> resultPage = new Page<>(memberPage.getCurrent(), memberPage.getSize(),
					memberPage.getTotal());
			resultPage.setRecords(voList);

			return resultPage;

		}

		return null;
	}

	@Override
	@Transactional
	public void addMemberForAdmin(AddMemberForAdminDTO addMemberForAdminDTO) {
		// 資料轉換
		Member member = memberConvert.forAdminAddDTOToEntity(addMemberForAdminDTO);
		// 判斷這個Email尚未被註冊
		LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		memberQueryWrapper.eq(Member::getEmail, member.getEmail());
		Long memberCount = baseMapper.selectCount(memberQueryWrapper);
		if (memberCount > 0) {
			throw new RegisteredAlreadyExistsException("This E-Mail has been registered");
		}

		// 新增會員
		baseMapper.insert(member);

		// 然後開始新建 繳費訂單
		AddOrdersDTO addOrdersDTO = new AddOrdersDTO();
		// 設定 會員ID
		addOrdersDTO.setMemberId(member.getMemberId());
		// 設定 這筆訂單商品的統稱
		addOrdersDTO.setItemsSummary(ITEMS_SUMMARY_REGISTRATION);
		// 設定繳費狀態為 已繳費(2)
		addOrdersDTO.setStatus(OrderStatusEnum.PAYMENT_SUCCESS.getValue());
		// 後台新增的會員(MVP)，不用繳費
		addOrdersDTO.setTotalAmount(BigDecimal.ZERO);
		// 透過訂單服務 新增訂單
		Long ordersId = ordersService.addOrders(addOrdersDTO);

		// 因為是綁在註冊時的訂單產生，所以這邊要再設定訂單的細節
		AddOrdersItemDTO addOrdersItemDTO = new AddOrdersItemDTO();
		// 設定 基本資料
		addOrdersItemDTO.setOrdersId(ordersId);
		addOrdersItemDTO.setProductType("Registration Fee");
		addOrdersItemDTO.setProductName("2025 TOPBS Registration Fee");

		// 設定 單價、數量、小計
		addOrdersItemDTO.setUnitPrice(BigDecimal.ZERO);
		addOrdersItemDTO.setQuantity(1);
		addOrdersItemDTO.setSubtotal(BigDecimal.ZERO);

		// 透過訂單明細服務 新增訂單
		ordersItemService.addOrdersItem(addOrdersItemDTO);

		/** ------------------------------------------------------- */

		//每200名會員設置一個tag, M-group-01, M-group-02(補零兩位數)
		String baseTagName = "M-group-%02d";
		// 分組數量
		Integer groupSize = 200;
		// groupIndex組別索引
		Integer groupIndex;

		//當前數量，上面已經新增過至少一人，不可能為0
		Long currentCount = baseMapper.selectCount(null);

		// 2. 計算組別 (向上取整，例如 201人 → 第2組)
		groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

		// 3. 生成 Tag 名稱 (補零兩位數)
		String tagName = String.format(baseTagName, groupIndex);
		String tagType = "member";

		// 4. 查詢是否已有該 Tag
		Tag existingTag = tagService.getTagByTypeAndName(tagType, tagName);

		// 5. 如果沒有就創建 Tag
		if (existingTag == null) {
			AddTagDTO addTagDTO = new AddTagDTO();
			addTagDTO.setType(tagType);
			addTagDTO.setName(tagName);
			addTagDTO.setDescription("會員分組標籤 (第 " + groupIndex + " 組)");
			addTagDTO.setStatus(0);
			String adjustColor = tagService.adjustColor("#4A7056", groupIndex, 5);
			addTagDTO.setColor(adjustColor);
			Long insertTagId = tagService.insertTag(addTagDTO);
			Tag currentTag = tagConvert.addDTOToEntity(addTagDTO);
			currentTag.setTagId(insertTagId);
			existingTag = currentTag;
		}

		// 6.透過tagId 去 關聯表 進行關聯新增
		MemberTag memberTag = new MemberTag();
		memberTag.setMemberId(member.getMemberId());
		memberTag.setTagId(existingTag.getTagId());
		memberTagService.addMemberTag(memberTag);

		/** ------------------------------------------------------- */

		// 由後台新增的Member，自動付款完成，新增進與會者名單
		AddAttendeesDTO addAttendeesDTO = new AddAttendeesDTO();
		addAttendeesDTO.setEmail(member.getEmail());
		addAttendeesDTO.setMemberId(member.getMemberId());
		attendeesService.addAfterPayment(addAttendeesDTO);
		
		
	}

	@Override
	@Transactional
	public SaTokenInfo addMember(AddMemberDTO addMemberDTO) throws RegistrationInfoException {

		// 獲取設定上的早鳥優惠、一般金額、及最後註冊時間
		Setting setting = settingService.getById(1L);

		// 獲取當前時間
		LocalDateTime now = LocalDateTime.now();

		//本次註冊是否是台灣人
		Boolean isTaiwan = addMemberDTO.getCountry().equals("Taiwan");

		// 先判斷是否超過註冊時間，當超出註冊時間直接拋出異常，讓全局異常去處理
		if (now.isAfter(setting.getLastRegistrationTime())) {
			throw new RegistrationClosedException("The registration time has ended, please register on site!");
		}

		// 設定會費 會根據早鳥優惠進行金額變動
		BigDecimal amount = null;

		// 處於早鳥優惠
		if (!now.isAfter(setting.getEarlyBirdDiscountPhaseOneDeadline())) {

			if (isTaiwan) {
				// 他是台灣人，當前時間處於早鳥優惠，金額變動
				amount = switch (addMemberDTO.getCategory()) {
				// Member(會員) 的註冊費價格
				case 1 -> BigDecimal.valueOf(700L);
				// Others(學生或護士) 的註冊費價格
				case 2 -> BigDecimal.valueOf(600L);
				// Non-Member(非會員) 的註冊費價格
				case 3 -> BigDecimal.valueOf(1000L);
				default -> throw new RegistrationInfoException("category is not in system");
				};
			} else {
				// 他是外國人，當前時間處於早鳥優惠，金額變動
				amount = switch (addMemberDTO.getCategory()) {
				// Member 的註冊費價格
				case 1 -> BigDecimal.valueOf(9600L);
				// Others 的註冊費價格
				case 2 -> BigDecimal.valueOf(4800L);
				// Non-member的註冊費價格
				case 3 -> BigDecimal.valueOf(12800L);
				default -> throw new RegistrationInfoException("category is not in system");
				};
			}

		} else if (
		// 時間比早鳥優惠時間晚 但比截止時間早，處於一般時間
		now.isAfter(setting.getEarlyBirdDiscountPhaseOneDeadline())
				&& now.isBefore(setting.getLastRegistrationTime())) {
			// 早鳥結束但尚未截止
			if (isTaiwan) {
				// 他是台灣人，當前時間處於一般時間，金額變動
				amount = switch (addMemberDTO.getCategory()) {
				// Member(會員) 的註冊費價格
				case 1 -> BigDecimal.valueOf(1000L);
				// Others(學生或護士) 的註冊費價格
				case 2 -> BigDecimal.valueOf(1200L);
				// Non-Member(非會員) 的註冊費價格
				case 3 -> BigDecimal.valueOf(1500L);
				default -> throw new RegistrationInfoException("category is not in system");
				};
			} else {
				// 他是外國人，當前時間處於一般時間，金額變動
				amount = switch (addMemberDTO.getCategory()) {
				// Member 的註冊費價格
				case 1 -> BigDecimal.valueOf(12800L);
				// Others 的註冊費價格
				case 2 -> BigDecimal.valueOf(6400L);
				// Non-member的註冊費價格
				case 3 -> BigDecimal.valueOf(16000L);
				default -> throw new RegistrationInfoException("category is not in system");
				};
			}
		}

		// 首先新增這個會員資料
		Member currentMember = memberConvert.addDTOToEntity(addMemberDTO);
		LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		memberQueryWrapper.eq(Member::getEmail, currentMember.getEmail());
		Long memberCount = baseMapper.selectCount(memberQueryWrapper);

		if (memberCount > 0) {
			throw new RegisteredAlreadyExistsException("This E-Mail has been registered");
		}

		baseMapper.insert(currentMember);

		// 然後開始新建 繳費訂單
		AddOrdersDTO addOrdersDTO = new AddOrdersDTO();
		// 設定 會員ID
		addOrdersDTO.setMemberId(currentMember.getMemberId());

		// 設定 這筆訂單商品的統稱
		addOrdersDTO.setItemsSummary(ITEMS_SUMMARY_REGISTRATION);

		// 設定繳費狀態為 未繳費(0)
		addOrdersDTO.setStatus(OrderStatusEnum.UNPAID.getValue());

		addOrdersDTO.setTotalAmount(amount);

		// 透過訂單服務 新增訂單
		Long ordersId = ordersService.addOrders(addOrdersDTO);

		// 因為是綁在註冊時的訂單產生，所以這邊要再設定訂單的細節
		AddOrdersItemDTO addOrdersItemDTO = new AddOrdersItemDTO();
		// 設定 基本資料
		addOrdersItemDTO.setOrdersId(ordersId);
		addOrdersItemDTO.setProductType("Registration Fee");
		addOrdersItemDTO.setProductName("2025 TOPBS Registration Fee");

		// 設定 單價、數量、小計
		addOrdersItemDTO.setUnitPrice(amount);
		addOrdersItemDTO.setQuantity(1);
		addOrdersItemDTO.setSubtotal(amount.multiply(BigDecimal.valueOf(1)));

		// 透過訂單明細服務 新增訂單
		ordersItemService.addOrdersItem(addOrdersItemDTO);

		// 準備寄信給這個會員通知他，已經成功註冊，所以先製作HTML信件 和 純文字信件

		String categoryString;
		switch (addMemberDTO.getCategory()) {
		case 1 -> categoryString = "Member " + "(" + addMemberDTO.getCategoryExtra() + ")";
		case 2 -> categoryString = "Others";
		case 3 -> categoryString = "Non-Member";
		default -> categoryString = "Unknown";
		}

		String htmlContent = """
				<!DOCTYPE html>
					<html >
						<head>
							<meta charset="UTF-8">
							<meta name="viewport" content="width=device-width, initial-scale=1.0">
							<title>Registration Successful</title>
							<style>
								body { font-size: 1.2rem; line-height: 1.8; }
								td { padding: 10px 0; }
							</style>
						</head>

						<body >
							<table>
								<tr>
					       			<td >
					           			<img src="https://iopbs2025.org.tw/_nuxt/banner.CL2lyu9P.png" alt="Conference Banner"  width="640" style="max-width: 100%%; width: 640px; display: block;" object-fit:cover;">
					       			</td>
					   			</tr>
								<tr>
									<td style="font-size:2rem;">Welcome to 2025 TOPBS & IOBPS !</td>
								</tr>
								<tr>
									<td>We are pleased to inform you that your registration has been successfully completed.</td>
								</tr>
								<tr>
									<td>Your registration details are as follows:</td>
								</tr>
								<tr>
						            <td><strong>First Name:</strong> %s</td>
						        </tr>
						        <tr>
						            <td><strong>Last Name:</strong> %s</td>
						        </tr>
						        <tr>
							        <td><strong>Country:</strong> %s</td>
							    </tr>
							    <tr>
							        <td><strong>Affiliation:</strong> %s</td>
							    </tr>
							    <tr>
							        <td><strong>Job Title:</strong> %s</td>
							    </tr>
							    <tr>
							        <td><strong>Phone:</strong> %s</td>
							    </tr>
							    <tr>
							        <td><strong>Category:</strong> %s</td>
							    </tr>
								<tr>
									<td>After logging in, please proceed with the payment of the registration fee.</td>
								</tr>
								<tr>
									<td>Completing this payment will grant you access to exclusive accommodation discounts and enable you to submit your work for the conference.</td>
								</tr>
								<tr>
									<td>If you have any questions, feel free to contact us. We look forward to seeing you at the conference!</td>
								</tr>
							</table>
						</body>
					</html>
					"""
				.formatted(addMemberDTO.getFirstName(), addMemberDTO.getLastName(), addMemberDTO.getCountry(),
						addMemberDTO.getAffiliation(), addMemberDTO.getJobTitle(), addMemberDTO.getPhone(),
						categoryString);

		String plainTextContent = "Welcome to 2025 TOPBS & IOBPS !\n"
				+ "Your registration has been successfully completed.\n" + "Your registration details are as follows:\n"
				+ "First Name: " + addMemberDTO.getFirstName() + "\n" + "Last Name: " + addMemberDTO.getLastName()
				+ "\n" + "Country: " + addMemberDTO.getCountry() + "\n" + "Affiliation: "
				+ addMemberDTO.getAffiliation() + "\n" + "Job Title: " + addMemberDTO.getJobTitle() + "\n" + "Phone: "
				+ addMemberDTO.getPhone() + "\n" + "Category: " + categoryString + "\n"
				+ "Please proceed with the payment of the registration fee to activate your accommodation discounts and submission features.\n"
				+ "If you have any questions, feel free to contact us. We look forward to seeing you at the conference!";

		// 透過異步工作去寄送郵件
		asyncService.sendCommonEmail(addMemberDTO.getEmail(), "2025 TOPBS & IOPBS  Registration Successful",
				htmlContent, plainTextContent);

		/** ------------------------------------------------------- */

		//每200名會員設置一個tag, M-group-01, M-group-02(補零兩位數)
		String baseTagName = "M-group-%02d";
		// 分組數量
		Integer groupSize = 200;
		// groupIndex組別索引
		Integer groupIndex;

		//當前數量，上面已經新增過至少一人，不可能為0
		Long currentCount = baseMapper.selectCount(null);

		// 2. 計算組別 (向上取整，例如 201人 → 第2組)
		groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

		// 3. 生成 Tag 名稱 (補零兩位數)
		String tagName = String.format(baseTagName, groupIndex);
		String tagType = "member";

		// 4. 查詢是否已有該 Tag
		Tag existingTag = tagService.getTagByTypeAndName(tagType, tagName);

		// 5. 如果沒有就創建 Tag
		if (existingTag == null) {
			AddTagDTO addTagDTO = new AddTagDTO();
			addTagDTO.setType(tagType);
			addTagDTO.setName(tagName);
			addTagDTO.setDescription("會員分組標籤 (第 " + groupIndex + " 組)");
			addTagDTO.setStatus(0);
			String adjustColor = tagService.adjustColor("#4A7056", groupIndex, 5);
			addTagDTO.setColor(adjustColor);
			Long insertTagId = tagService.insertTag(addTagDTO);
			Tag currentTag = tagConvert.addDTOToEntity(addTagDTO);
			currentTag.setTagId(insertTagId);
			existingTag = currentTag;
		}

		// 6.透過tagId 去 關聯表 進行關聯新增
		MemberTag memberTag = new MemberTag();
		memberTag.setMemberId(currentMember.getMemberId());
		memberTag.setTagId(existingTag.getTagId());
		memberTagService.addMemberTag(memberTag);

		/** ------------------------------------------------------- */

		// 之後應該要以這個會員ID 產生Token 回傳前端，讓他直接進入登入狀態
		StpKit.MEMBER.login(currentMember.getMemberId());

		// 登入後才能取得session
		SaSession session = StpKit.MEMBER.getSession();
		// 並對此token 設置會員的緩存資料
		session.set(MEMBER_CACHE_INFO_KEY, currentMember);

		SaTokenInfo tokenInfo = StpKit.MEMBER.getTokenInfo();
		return tokenInfo;

	}

	@Override
	@Transactional
	public void addGroupMember(GroupRegistrationDTO groupRegistrationDTO) {

		// 獲取設定上的早鳥優惠、一般金額、及最後註冊時間
		Setting setting = settingService.getById(1L);

		// 獲取當前時間
		LocalDateTime now = LocalDateTime.now();

		// 先判斷是否超過團體註冊時間(也就是早鳥一階段時間)，當超出團體註冊時間直接拋出異常，讓全局異常去處理
		if (now.isAfter(setting.getEarlyBirdDiscountPhaseOneDeadline())) {
			throw new RegistrationClosedException("The group registration time has ended");
		}

		// 用於累計所有成員的費用總和
		BigDecimal totalFee = BigDecimal.ZERO;

		// 在外部先獲取整個團體報名名單
		List<AddGroupMemberDTO> groupMembers = groupRegistrationDTO.getGroupMembers();

		// 在外部直接產生團體的代號
		String groupCode = UUID.randomUUID().toString();

		// 在外部紀錄第一位主報名者的memberId
		Long firstMasterId = 1L;

		for (int i = 0; i < groupRegistrationDTO.getGroupMembers().size(); i++) {
			// 獲取名單內成員,轉換成Entity對象，並把他設定group欄位
			AddGroupMemberDTO addGroupMemberDTO = groupMembers.get(i);
			Member member = memberConvert.addGroupDTOToEntity(addGroupMemberDTO);
			member.setGroupCode(groupCode);

			// 先建立當前會員的費用
			BigDecimal currentAmount;

			currentAmount = switch (member.getCategory()) {
			// Member 的註冊費價格
			case 1 -> BigDecimal.valueOf(9600L);
			// Others 的註冊費價格
			case 2 -> BigDecimal.valueOf(4800L);
			// Non-Member 的註冊費價格
			case 3 -> BigDecimal.valueOf(12800L);
			default -> throw new RegistrationInfoException("category is not in system");
			};

			// 加總每位會員金額的總額
			totalFee = totalFee.add(currentAmount);

			// 第一位報名者為主報名者(master)，其餘為從屬(slave)
			if (i == 0) {
				member.setGroupRole("master");
				baseMapper.insert(member);
				firstMasterId = member.getMemberId();
			} else {
				// 其餘的團體報名者都是子報名者(slave)
				member.setGroupRole("slave");
				baseMapper.insert(member);

				// 開始對子報名者做訂單和訂單明細生成
				AddOrdersDTO addOrdersDTO = new AddOrdersDTO();
				// 為訂單設定 會員ID
				addOrdersDTO.setMemberId(member.getMemberId());

				// 設定 這筆訂單商品的統稱
				addOrdersDTO.setItemsSummary(GROUP_ITEMS_SUMMARY_REGISTRATION);

				// 設定繳費狀態為 未繳費(0) ， 團體費用為 0 ，因為真正的金額會計算在主報名者身上
				addOrdersDTO.setStatus(OrderStatusEnum.UNPAID.getValue());
				addOrdersDTO.setTotalAmount(BigDecimal.ZERO);

				// 透過訂單服務 新增訂單
				Long ordersId = ordersService.addOrders(addOrdersDTO);

				// 因為是綁在註冊時的訂單產生，所以這邊要再設定訂單的細節
				AddOrdersItemDTO addOrdersItemDTO = new AddOrdersItemDTO();
				// 設定 基本資料
				addOrdersItemDTO.setOrdersId(ordersId);
				addOrdersItemDTO.setProductType("Registration Fee");
				addOrdersItemDTO.setProductName("2025 TOPBS Group Registration Fee");

				// 設定 單價、數量、小計
				addOrdersItemDTO.setUnitPrice(BigDecimal.ZERO);
				addOrdersItemDTO.setQuantity(1);
				addOrdersItemDTO.setSubtotal(BigDecimal.ZERO);

				// 透過訂單明細服務 新增訂單
				ordersItemService.addOrdersItem(addOrdersItemDTO);

				// 寄信給這個會員通知他，已經成功註冊；這是使用異步線程執行
				asyncService.sendGroupRegistrationEmail(member);

			}

			/** ------------------------------------------------------- */

			//每200名會員設置一個tag, M-group-01, M-group-02(補零兩位數)
			String baseTagName = "M-group-%02d";
			// 分組數量
			Integer groupSize = 200;
			// groupIndex組別索引
			Integer groupIndex;

			//當前數量，上面已經新增過至少一人，不可能為0
			Long currentCount = baseMapper.selectCount(null);

			// 2. 計算組別 (向上取整，例如 201人 → 第2組)
			groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

			// 3. 生成 Tag 名稱 (補零兩位數)
			String tagName = String.format(baseTagName, groupIndex);
			String tagType = "member";

			// 4. 查詢是否已有該 Tag
			Tag existingTag = tagService.getTagByTypeAndName(tagType, tagName);

			// 5. 如果沒有就創建 Tag
			if (existingTag == null) {
				AddTagDTO addTagDTO = new AddTagDTO();
				addTagDTO.setType(tagType);
				addTagDTO.setName(tagName);
				addTagDTO.setDescription("會員分組標籤 (第 " + groupIndex + " 組)");
				addTagDTO.setStatus(0);
				String adjustColor = tagService.adjustColor("#4A7056", groupIndex, 5);
				addTagDTO.setColor(adjustColor);
				Long insertTagId = tagService.insertTag(addTagDTO);
				Tag currentTag = tagConvert.addDTOToEntity(addTagDTO);
				currentTag.setTagId(insertTagId);
				existingTag = currentTag;
			}

			// 6.透過tagId 去 關聯表 進行關聯新增
			MemberTag memberTag = new MemberTag();
			memberTag.setMemberId(member.getMemberId());
			memberTag.setTagId(existingTag.getTagId());
			memberTagService.addMemberTag(memberTag);

			/** ------------------------------------------------------- */

		}

		// 已經拿到所有金額了, 這時對第一位主報名者(master) 做訂單和訂單明細的生成
		// 計算 9 折後的金額
		BigDecimal discountedTotalFee = totalFee.multiply(BigDecimal.valueOf(0.9));
		AddOrdersDTO addOrdersDTO = new AddOrdersDTO();
		// 設定 會員ID
		addOrdersDTO.setMemberId(firstMasterId);

		// 設定 這筆訂單商品的統稱
		addOrdersDTO.setItemsSummary(GROUP_ITEMS_SUMMARY_REGISTRATION);

		// 設定繳費狀態為 未繳費(0)
		// 團體費用為總費用打九折(團體報名折扣) ，因為會計算在主報名者身上
		addOrdersDTO.setStatus(OrderStatusEnum.UNPAID.getValue());
		addOrdersDTO.setTotalAmount(discountedTotalFee);

		// 透過訂單服務 新增訂單
		Long ordersId = ordersService.addOrders(addOrdersDTO);

		// 因為是綁在註冊時的訂單產生，所以這邊要再設定訂單的細節
		AddOrdersItemDTO addOrdersItemDTO = new AddOrdersItemDTO();
		// 設定 基本資料
		addOrdersItemDTO.setOrdersId(ordersId);
		addOrdersItemDTO.setProductType("Registration Fee");
		addOrdersItemDTO.setProductName("2025 TOPBS Group Registration Fee");

		// 設定 單價、數量、小計
		addOrdersItemDTO.setUnitPrice(discountedTotalFee);
		addOrdersItemDTO.setQuantity(1);
		addOrdersItemDTO.setSubtotal(discountedTotalFee.multiply(BigDecimal.valueOf(1)));

		// 透過訂單明細服務 新增訂單
		ordersItemService.addOrdersItem(addOrdersItemDTO);

		// 查詢一下主報名者 master 的資料
		Member firstMaster = baseMapper.selectById(firstMasterId);

		// 寄信給這個會員通知他，已經成功註冊
		// 開始編寫信件,準備寄給一般註冊者找回密碼的信
		asyncService.sendGroupRegistrationEmail(firstMaster);

	}

	@Override
	public void updateMember(PutMemberDTO putMemberDTO) {
		Member member = memberConvert.putDTOToEntity(putMemberDTO);
		baseMapper.updateById(member);
	}

	@Override
	public void approveUnpaidMember(Long memberId) {
		// 更新狀態為已付款
		ordersManager.approveUnpaidMember(memberId);
		
		// 拿到Member資訊
		Member member = baseMapper.selectById(memberId);
		
		// 付款完成，新增進與會者名單
		AddAttendeesDTO addAttendeesDTO = new AddAttendeesDTO();
		addAttendeesDTO.setEmail(member.getEmail());
		addAttendeesDTO.setMemberId(member.getMemberId());
		attendeesService.addAfterPayment(addAttendeesDTO);
	}

	@Override
	public void deleteMember(Long memberId) {
		baseMapper.deleteById(memberId);
	}

	@Override
	public void deleteMemberList(List<Long> memberIds) {
		baseMapper.deleteBatchIds(memberIds);
	}

	@Override
	public Member getMemberInfo() {
		// 會員登入後才能取得session
		SaSession session = StpKit.MEMBER.getSession();
		// 獲取當前使用者的資料
		Member memberInfo = (Member) session.get(MEMBER_CACHE_INFO_KEY);
		return memberInfo;
	}

	@Override
	public void downloadExcel(HttpServletResponse response) throws IOException {
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("會員名單", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 先查詢所有沒被刪除 且 items_summary為 註冊費 或者 團體註冊費 訂單， 這種名稱只會出現一次，且不會同時出現
		List<Orders> ordersList = ordersManager.getRegistrationOrderListForExcel();

		// 訂單轉成一對一 Map，key為 memberId, value為訂單本身
		//如果你需要將流中的每一個元素（此處為 Orders）放入 Map 且不需要進行額外的轉換，
		//就可以使用 Function.identity()。它是最簡單的一種方式，表示"元素本身就是值"，省去了額外的映射步驟。
		Map<Long, Orders> ordersMap = ordersList.stream()
				.collect(Collectors.toMap(Orders::getMemberId, Function.identity()));

		// 查詢所有會員，Excel數據就是以他為依據的
		List<Member> memberList = baseMapper.selectMembers();

		List<MemberExcel> excelData = memberList.stream().map(member -> {
			Orders orders = ordersMap.get(member.getMemberId());

			MemberExcelRaw memberExcelRaw = memberConvert.entityToExcelRaw(member);
			memberExcelRaw.setStatus(orders.getStatus());
			memberExcelRaw.setRegistrationFee(orders.getTotalAmount());

			MemberExcel memberExcel = memberConvert.memberExcelRawToExcel(memberExcelRaw);

			return memberExcel;

		}).toList();

		EasyExcel.write(response.getOutputStream(), MemberExcel.class).sheet("會員列表").doWrite(excelData);

		//		
		//		  // 测量第一部分执行时间
		//		  // long startTime1 = System.nanoTime();
		//		  // 第一部分代码
		//		
		//		List<Member> member = baseMapper.selectList(null);
		//		  
		//		  // long endTime1 = System.nanoTime();
		//		  
		//		  // System.out.println("第一部分执行时间: " + (endTime1 - startTime1) // 1_000_000_000.0 + " 秒");
		//		  
		//		  System.out.println("--------接下來轉換數據------------");
		//		  
		//		  // 测量第二部分执行时间
		//		 // long startTime2 = System.nanoTime();
		//		  
		//		  List<MemberExcel> excelData =
		//		  organDonationConsentList.stream().map(organDonationConsent -> {
		//		  return organDonationConsentConvert.entityToExcel(organDonationConsent);
		//		  }).collect(Collectors.toList());
		//		  
		//		  // long endTime2 = System.nanoTime();
		//		  
		//		  // System.out.println("第二部分执行时间: " + (endTime2 - startTime2) /
		//		  1_000_000_000.0 + " 秒");
		//		  
		//		  System.out.println("接下來寫入數據");
		//		  
		//		  // 测量第三部分执行时间
		//		  // long startTime3 = System.nanoTime();
		//		  
		//		  EasyExcel.write(response.getOutputStream(),
		//		  MemberExcel.class).sheet("會員列表").doWrite(excelData);
		//		  
		//		  // long endTime3 = System.nanoTime();
		//		  // System.out.println("第三部分执行时间: " + (endTime3 - startTime3) /
		//		  1_000_000_000.0 + " 秒");
		//		  
		//		

	}

	/** 以下跟登入有關 */
	@Override
	public SaTokenInfo login(MemberLoginInfo memberLoginInfo) {
		LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		memberQueryWrapper.eq(Member::getEmail, memberLoginInfo.getEmail())
				.eq(Member::getPassword, memberLoginInfo.getPassword());

		Member member = baseMapper.selectOne(memberQueryWrapper);

		if (member != null) {
			// 之後應該要以這個會員ID 產生Token 回傳前端，讓他直接進入登入狀態
			StpKit.MEMBER.login(member.getMemberId());

			// 登入後才能取得session
			SaSession session = StpKit.MEMBER.getSession();
			// 並對此token 設置會員的緩存資料
			session.set(MEMBER_CACHE_INFO_KEY, member);
			SaTokenInfo tokenInfo = StpKit.MEMBER.getTokenInfo();

			return tokenInfo;
		}

		// 如果 member為null , 則直接拋出異常
		throw new AccountPasswordWrongException("Wrong account or password");

	}

	@Override
	public void logout() {
		// 根據token 直接做登出
		StpKit.MEMBER.logout();

	}

	@Override
	public void forgetPassword(String email) throws MessagingException {

		// 透過Email查詢Member
		LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
		memberQueryWrapper.eq(Member::getEmail, email);

		Member member = baseMapper.selectOne(memberQueryWrapper);
		// 如果沒找到該email的member，則直接丟異常給全局處理
		if (member == null) {
			throw new ForgetPasswordException("No such email found");
		}

		// 設置信件 html Content
		String htmlContent = """
				<!DOCTYPE html>
				<html >
				<head>
					<meta charset="UTF-8">
					<meta name="viewport" content="width=device-width, initial-scale=1.0">
					<title>Retrieve password</title>
				</head>

				<body >
					<table>
				    	<tr>
				        	<td style="font-size:1.5rem;" >Retrieve password for you</td>
				        </tr>
				        <tr>
				            <td>your password is：<strong>%s</strong></td>
				        </tr>
				        <tr>
				            <td>Please record your password to avoid losing it again.</td>
				        </tr>
				        <tr>
				            <td>If you have not requested password retrieval, please ignore this email.</td>
				        </tr>
				    </table>
				</body>
				</html>
				""".formatted(member.getPassword());

		String plainTextContent = "your password is：" + member.getPassword()
				+ "\n Please record your password to avoid losing it again \n If you have not requested password retrieval, please ignore this email.";

		// 透過異步工作去寄送郵件
		asyncService.sendCommonEmail(email, "Retrieve password", htmlContent, plainTextContent);

	}

	/** 以下跟Tag有關 */
	@Override
	public MemberTagVO getMemberTagVOByMember(Long memberId) {

		// 1.獲取member 資料並轉換成 memberTagVO
		Member member = baseMapper.selectById(memberId);
		MemberTagVO memberTagVO = memberConvert.entityToMemberTagVO(member);

		// 2.查詢該member所有關聯的tag
		List<MemberTag> memberTagList = memberTagService.getMemberTagByMemberId(memberId);

		// 如果沒有任何關聯,就可以直接返回了
		if (memberTagList.isEmpty()) {
			return memberTagVO;
		}

		// 3.獲取到所有memberTag的關聯關係後，提取出tagIdList
		List<Long> tagIdList = memberTagList.stream()
				.map(memberTag -> memberTag.getTagId())
				.collect(Collectors.toList());

		// 4.去Tag表中查詢實際的Tag資料，並轉換成Set集合
		List<Tag> tagList = tagService.getTagByTagIds(tagIdList);

		Set<Tag> tagSet = new HashSet<>(tagList);

		// 5.最後填入memberTagVO對象並返回
		memberTagVO.setTagSet(tagSet);
		return memberTagVO;
	}

	@Override
	public IPage<MemberTagVO> getAllMemberTagVO(Page<Member> page) {

		IPage<MemberTagVO> voPage;

		// 1.以member當作基底查詢,越新的擺越前面
		LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
		memberWrapper.orderByDesc(Member::getMemberId);

		// 2.查詢 MemberPage (分頁)
		IPage<Member> memberPage = baseMapper.selectPage(page, memberWrapper);

		// 3. 獲取所有 memberId 列表，
		List<Long> memberIds = memberPage.getRecords().stream().map(Member::getMemberId).collect(Collectors.toList());

		if (memberIds.isEmpty()) {
			System.out.println("沒有會員,所以直接返回");

			voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
			voPage.setRecords(null);

			return voPage;
		}

		// 4. 批量查詢 MemberTag 關係表，獲取 memberId 对应的 tagId
		List<MemberTag> memberTagList = memberTagService.getMemberTagByMemberIds(memberIds);

		// 5. 將 memberId 對應的 tagId 歸類，key 為memberId , value 為 tagIdList
		Map<Long, List<Long>> memberTagMap = memberTagList.stream()
				.collect(Collectors.groupingBy(MemberTag::getMemberId,
						Collectors.mapping(MemberTag::getTagId, Collectors.toList())));

		// 6. 獲取所有 tagId 列表
		List<Long> tagIds = memberTagList.stream().map(MemberTag::getTagId).distinct().collect(Collectors.toList());

		// 7. 批量查詢所有的 Tag，如果關聯的tagIds為空, 那就不用查了，直接返回
		if (tagIds.isEmpty()) {
			System.out.println("沒有任何tag關聯,所以直接返回");
			List<MemberTagVO> memberTagVOList = memberPage.getRecords().stream().map(member -> {
				MemberTagVO vo = memberConvert.entityToMemberTagVO(member);
				vo.setTagSet(new HashSet<>());

				// 找到items_summary 符合 Registration Fee 以及 訂單會員ID與 會員相符的資料
				Orders memberOrder = ordersManager.getRegistrationOrderByMemberId(member.getMemberId());
				// 取出status 並放入VO對象中
				vo.setStatus(memberOrder.getStatus());

				return vo;
			}).collect(Collectors.toList());
			voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
			voPage.setRecords(memberTagVOList);

			return voPage;

		}

		List<Tag> tagList = tagService.getTagByTagIds(tagIds);

		// 8. 將 Tag 按 tagId 歸類
		Map<Long, Tag> tagMap = tagList.stream().collect(Collectors.toMap(Tag::getTagId, tag -> tag));

		// 9. 組裝 VO 數據
		List<MemberTagVO> voList = memberPage.getRecords().stream().map(member -> {
			MemberTagVO vo = memberConvert.entityToMemberTagVO(member);
			// 獲取該 memberId 關聯的 tagId 列表
			List<Long> relatedTagIds = memberTagMap.getOrDefault(member.getMemberId(), Collections.emptyList());
			// 獲取所有對應的 Tag
			List<Tag> tags = relatedTagIds.stream()
					.map(tagMap::get)
					.filter(Objects::nonNull) // 避免空值
					.collect(Collectors.toList());
			Set<Tag> tagSet = new HashSet<>(tags);
			vo.setTagSet(tagSet);
			return vo;
		}).collect(Collectors.toList());

		// 10. 重新封装 VO 的分頁對象
		voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
		voPage.setRecords(voList);

		return voPage;
	}

	@Override
	public IPage<MemberTagVO> getAllMemberTagVOByQuery(Page<Member> page, String queryText, Integer status) {

		IPage<MemberTagVO> voPage;
		List<Long> memberIdsByStatus = new ArrayList<>();

		// 1.如果有status 參數，則要先抓出來當作member的篩選條件
		if (status != null) {

			// 找到items_summary 符合 Registration Fee ，且status符合篩選條件的資料
			List<Orders> orderList = ordersManager.getRegistrationOrderListByStatus(status);

			// 擷取出符合status 參數的會員
			memberIdsByStatus = orderList.stream().map(order -> order.getMemberId()).collect(Collectors.toList());
			System.out.println("符合status:" + status + "的資料， " + memberIdsByStatus);

			// 如果找不到符合的會員 ID，直接 return 空頁面
			if (memberIdsByStatus.isEmpty()) {

				//				System.out.println("沒有會員,所以直接返回");
				//				voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
				//				voPage.setRecords(null);
				//				return voPage;

				IPage<MemberTagVO> emptyPage = new Page<>(page.getCurrent(), page.getSize(), 0);
				emptyPage.setRecords(Collections.emptyList());
				return emptyPage;
			}

		}

		// 2.基於條件查詢 memberList
		LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();

		// 當 queryText 不為空字串、空格字串、Null 時才加入篩選條件
		// 且memberIdsByStatus裡面元素不為空，則加入篩選條件
		memberWrapper
				.and(StringUtils.isNotBlank(queryText),
						wrapper -> wrapper.like(Member::getFirstName, queryText)
								.or()
								.like(Member::getLastName, queryText)
								.or()
								.like(Member::getChineseName, queryText)
								.or()
								.like(Member::getPhone, queryText)
								.or()
								.like(Member::getRemitAccountLast5, queryText))
				.in(!memberIdsByStatus.isEmpty(), Member::getMemberId, memberIdsByStatus);

		// 3.查詢 MemberPage (分頁)
		IPage<Member> memberPage = baseMapper.selectPage(page, memberWrapper);
		System.out.println("查詢到的memberPage: " + memberPage);

		// 4. 獲取所有 memberId 列表，
		List<Long> memberIds = memberPage.getRecords().stream().map(Member::getMemberId).collect(Collectors.toList());

		if (memberIds.isEmpty()) {
			System.out.println("沒有會員,所以直接返回");

			voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
			voPage.setRecords(null);
			return voPage;

		}

		// 5. 批量查詢 MemberTag 關係表，獲取 memberId 对应的 tagId
		List<MemberTag> memberTagList = memberTagService.getMemberTagByMemberIds(memberIds);

		// 6. 將 memberId 對應的 tagId 歸類，key 為memberId , value 為 tagIdList
		Map<Long, List<Long>> memberTagMap = memberTagList.stream()
				.collect(Collectors.groupingBy(MemberTag::getMemberId,
						Collectors.mapping(MemberTag::getTagId, Collectors.toList())));

		// 7. 獲取所有 tagId 列表
		List<Long> tagIds = memberTagList.stream().map(MemberTag::getTagId).distinct().collect(Collectors.toList());

		// 8. 批量查询所有的 Tag，如果關聯的tagIds為空, 那就不用查了，直接返回
		if (tagIds.isEmpty()) {

			System.out.println("沒有任何tag關聯,所以直接返回");
			List<MemberTagVO> memberTagVOList = memberPage.getRecords().stream().map(member -> {

				MemberTagVO vo = memberConvert.entityToMemberTagVO(member);
				vo.setTagSet(new HashSet<>());

				// 找到items_summary 符合 Registration Fee 或者 Group Registration Fee 以及 訂單會員ID與 會員相符的資料
				Orders memberOrder = ordersManager.getRegistrationOrderByMemberId(member.getMemberId());

				// 取出status 並放入VO對象中
				vo.setStatus(memberOrder.getStatus());

				return vo;
			}).collect(Collectors.toList());
			voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
			voPage.setRecords(memberTagVOList);
			return voPage;

		}

		List<Tag> tagList;

		// 在這裡再帶入關於Tag的查詢條件，
		//		if (!tags.isEmpty()) {
		//			// 如果傳來的tags不為空 , 直接使用前端傳來的id列表當作搜尋條件
		//			tagList = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getTagId, tags));
		//		} else {
		//			// 如果傳來的tags為空 ， 則使用跟memberList關聯的tagIds 查詢
		//			tagList = tagMapper.selectList(new LambdaQueryWrapper<Tag>().in(Tag::getTagId, tagIds));
		//		}

		tagList = tagService.getTagByTagIds(tagIds);

		// 9. 將 Tag 按 tagId 歸類
		Map<Long, Tag> tagMap = tagList.stream().collect(Collectors.toMap(Tag::getTagId, tag -> tag));

		// 10. 組裝 VO 數據
		List<MemberTagVO> voList = memberPage.getRecords().stream().map(member -> {

			// 將查找到的Member,轉換成VO對象
			MemberTagVO vo = memberConvert.entityToMemberTagVO(member);
			// 獲取該 memberId 關聯的 tagId 列表
			List<Long> relatedTagIds = memberTagMap.getOrDefault(member.getMemberId(), Collections.emptyList());
			// 獲取所有對應的 Tag
			List<Tag> allTags = relatedTagIds.stream()
					.map(tagMap::get)
					.filter(Objects::nonNull) // 避免空值
					.collect(Collectors.toList());
			Set<Tag> tagSet = new HashSet<>(allTags);

			// 將 tagSet 放入VO中
			vo.setTagSet(tagSet);

			// 找到items_summary 符合 Registration Fee 以及 訂單會員ID與 會員相符的資料
			Orders memberOrder = ordersManager.getRegistrationOrderByMemberId(member.getMemberId());

			// 取出status 並放入VO對象中
			vo.setStatus(memberOrder.getStatus());

			return vo;
		}).collect(Collectors.toList());

		// 10. 重新封装 VO 的分頁對象
		voPage = new Page<>(page.getCurrent(), page.getSize(), memberPage.getTotal());
		voPage.setRecords(voList);

		return voPage;
	}

	@Transactional
	@Override
	public void assignTagToMember(List<Long> targetTagIdList, Long memberId) {
		// 1. 查詢當前 member 的所有關聯 tag
		List<MemberTag> currentMemberTags = memberTagService.getMemberTagByMemberId(memberId);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentMemberTags.stream().map(MemberTag::getTagId).collect(Collectors.toSet());

		// 3. 對比目標 memberIdList 和當前 memberIdList
		Set<Long> targetTagIdSet = new HashSet<>(targetTagIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> tagsToRemove = new HashSet<>(currentTagIdSet);
		// 差集：當前有但目標沒有
		tagsToRemove.removeAll(targetTagIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> tagsToAdd = new HashSet<>(targetTagIdSet);
		// 差集：目標有但當前沒有
		tagsToAdd.removeAll(currentTagIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!tagsToRemove.isEmpty()) {
			memberTagService.removeTagsFromMember(memberId, tagsToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			List<MemberTag> newMemberTags = tagsToAdd.stream().map(tagId -> {
				MemberTag memberTag = new MemberTag();
				memberTag.setTagId(tagId);
				memberTag.setMemberId(memberId);
				return memberTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (MemberTag memberTag : newMemberTags) {
				memberTagService.addMemberTag(memberTag);
			}
		}

	}

	@Override
	public void sendEmailToMembers(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {
		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);

		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有會員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的會員人數
		Long memberCount = 0L;

		//初始化要寄信的會員
		List<Member> memberList = new ArrayList<>();

		//初始化 memberIdSet ，用於去重memberId
		Set<Long> memberIdSet = new HashSet<>();

		if (hasNoTag) {
			memberCount = baseMapper.selectCount(null);
		} else {
			// 透過tag先找到符合的member關聯
			List<MemberTag> memberTagList = memberTagService.getMemberTagByTagIds(tagIdList);

			// 從關聯中取出memberId ，使用Set去重複的會員，因為會員有可能有多個Tag
			memberIdSet = memberTagList.stream().map(memberTag -> memberTag.getMemberId()).collect(Collectors.toSet());

			// 如果memberIdSet 至少有一個，則開始搜尋Member
			if (!memberIdSet.isEmpty()) {
				LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
				memberWrapper.in(Member::getMemberId, memberIdSet);
				memberCount = baseMapper.selectCount(memberWrapper);
			}

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (currentQuota < memberCount) {
			throw new EmailException("本日寄信額度剩餘: " + currentQuota + "，無法寄送 " + memberCount + " 封信");
		} else if (memberCount <= 0) {
			throw new EmailException("沒有符合資格的會員");
		}

		// 前面都已經透過總數先排除了 額度不足、沒有符合資格會員的狀況，現在實際來獲取收信者名單
		// 沒有篩選任何Tag的，則給他所有Member名單
		if (hasNoTag) {
			memberList = baseMapper.selectList(null);
		} else {

			// 如果memberIdSet 至少有一個，則開始搜尋Member
			if (!memberIdSet.isEmpty()) {
				LambdaQueryWrapper<Member> memberWrapper = new LambdaQueryWrapper<>();
				memberWrapper.in(Member::getMemberId, memberIdSet);
				memberList = baseMapper.selectList(memberWrapper);
			}

		}

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		asyncService.batchSendEmailToMembers(memberList, sendEmailDTO);

		// 額度直接扣除 查詢到的會員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-memberCount);

	}

}
