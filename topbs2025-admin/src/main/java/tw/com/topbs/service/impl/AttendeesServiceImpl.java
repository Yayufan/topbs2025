package tw.com.topbs.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Strings;
import com.google.zxing.WriterException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.convert.CheckinRecordConvert;
import tw.com.topbs.enums.CheckinActionTypeEnum;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.manager.AttendeesManager;
import tw.com.topbs.manager.CheckinRecordManager;
import tw.com.topbs.manager.MemberManager;
import tw.com.topbs.manager.OrdersItemManager;
import tw.com.topbs.manager.OrdersManager;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.BO.PresenceStatsBO;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.WalkInRegistrationDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddAttendeesDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddCheckinRecordDTO;
import tw.com.topbs.pojo.VO.AttendeesStatsVO;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.VO.CheckinRecordVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.pojo.entity.CheckinRecord;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.pojo.excelPojo.AttendeesExcel;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.ScheduleEmailTaskService;
import tw.com.topbs.service.TagService;
import tw.com.topbs.utils.QrcodeUtil;

/**
 * <p>
 * 參加者表，在註冊並實際繳完註冊費後，會進入這張表中，用做之後發送QRcdoe使用 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-04-24
 */
@Service
@RequiredArgsConstructor
public class AttendeesServiceImpl extends ServiceImpl<AttendeesMapper, Attendees> implements AttendeesService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";

	private final MemberManager memberManager;
	private final MemberTagService memberTagService;
	private final OrdersManager ordersManager;
	private final OrdersItemManager ordersItemManager;
	private final CheckinRecordManager checkinRecordManager;
	private final CheckinRecordConvert checkinRecordConvert;
	private final AttendeesManager attendeesManager;
	private final AttendeesConvert attendeesConvert;
	private final AttendeesTagService attendeesTagService;
	private final TagService tagService;
	private final AsyncService asyncService;
	private final ScheduleEmailTaskService scheduleEmailTaskService;

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public AttendeesVO getAttendees(Long id) {
		AttendeesVO attendeesVO = attendeesManager.getAttendeesVOByAttendeesId(id);
		return attendeesVO;
	}

	@Override
	public List<AttendeesVO> getAttendeesList() {
		// 1.查詢所有 attendees
		List<Attendees> attendeesList = baseMapper.selectList(null);

		// 2.透過私有方法轉換成VOList
		List<AttendeesVO> attendeesVOList = this.convertToAttendeesVO(attendeesList);

		return attendeesVOList;
	}

	@Override
	public IPage<AttendeesVO> getAttendeesPage(Page<Attendees> page) {
		// 1.查詢所有 attendees 分頁對象
		Page<Attendees> attendeesPage = baseMapper.selectPage(page, null);

		// 2.透過私有方法轉換成VOList
		List<AttendeesVO> attendeesVOList = this.convertToAttendeesVO(attendeesPage.getRecords());

		// 3.封裝成VOpage
		Page<AttendeesVO> attendeesVOPage = new Page<>(attendeesPage.getCurrent(), attendeesPage.getSize(),
				attendeesPage.getTotal());
		attendeesVOPage.setRecords(attendeesVOList);

		return attendeesVOPage;
	}

	/**
	 * 私有方法用來將attendeesList 轉換成 attendeesVOList
	 * 
	 * @param attendeesList
	 * @return
	 */
	private List<AttendeesVO> convertToAttendeesVO(List<Attendees> attendeesList) {

		// 1.從attendeesList提取memberIds , 避免全表查詢member
		Set<Long> memberIds = attendeesList.stream().map(Attendees::getMemberId).collect(Collectors.toSet());

		// 2.透過AttendeesList中提取的memberIds 中找到與會者的基本資料
		List<Member> memberList = memberManager.getMembersByIds(memberIds);

		// 3.建立映射
		Map<Long, Member> memberMap = memberList.stream()
				.collect(Collectors.toMap(Member::getMemberId, Function.identity()));

		// 4.資料轉換成VO
		List<AttendeesVO> attendeesVOList = attendeesList.stream().map(attendees -> {
			AttendeesVO vo = attendeesConvert.entityToVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			return vo;
		}).collect(Collectors.toList());

		return attendeesVOList;
	}

	@Override
	public AttendeesStatsVO getAttendeesStatsVO() {

		AttendeesStatsVO attendeesStatsVO = new AttendeesStatsVO();

		//查詢 應到 人數
		Integer countTotalShouldAttend = baseMapper.countTotalShouldAttend();
		attendeesStatsVO.setTotalShouldAttend(countTotalShouldAttend);

		//查詢 已簽到 人數
		Integer countCheckedIn = checkinRecordManager.getCountCheckedIn();
		attendeesStatsVO.setTotalCheckedIn(countCheckedIn);

		//未簽到人數
		attendeesStatsVO.setTotalNotArrived(countTotalShouldAttend - countCheckedIn);

		//查詢 尚在現場、已離場 人數
		PresenceStatsBO presenceStatsBO = checkinRecordManager.getPresenceStats();
		attendeesStatsVO.setTotalOnSite(presenceStatsBO.getTotalOnsite());
		attendeesStatsVO.setTotalLeft(presenceStatsBO.getTotalLeft());

		return attendeesStatsVO;
	}

	@Transactional
	@Override
	public CheckinRecordVO walkInRegistration(WalkInRegistrationDTO walkInRegistrationDTO)
			throws Exception, IOException {

		// 1.創建Member對象，新增進member table
		Member member = memberManager.addMemberOnSite(walkInRegistrationDTO);

		// 2.創建已繳費訂單-預設他會在現場繳費完成
		Orders orders = ordersManager.createZeroAmountRegistrationOrder(member.getMemberId());

		// 3.因為是綁在註冊時的訂單產生，所以這邊要再設定訂單的細節
		ordersItemManager.addRegistrationOrderItem(orders.getOrdersId(), orders.getTotalAmount());

		// 4. 計算目前會員數量 → 分組索引
		Long currentCount = memberManager.getMemberCount();
		int groupSize = 200;
		int groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

		// 5. 呼叫 Manager 拿到 Tag（不存在則新增Tag）
		Tag groupTag = tagService.getOrCreateMemberGroupTag(groupIndex);

		// 6. 關聯 Member 與 Tag
		memberTagService.addMemberTag(member.getMemberId(), groupTag.getTagId());

		// 7. 創建與會者 和 簽到記錄，製作返回簽到時的格式
		CheckinRecordVO checkinRecordVO = this.createAttendeeAndCheckin(member);

		// 8.創建一個寄送QRcode的Mail給現場註冊登入的來賓

		// 8-1.製作HTML信件，並帶入QRcode 生成的API在img src屬性
		String htmlContent = """
				<!DOCTYPE html>
					<html >
						<head>
							<meta charset="UTF-8">
							<meta name="viewport" content="width=device-width, initial-scale=1.0">
							<title>現場登錄成功通知</title>
							<style>
								body { font-size: 1.2rem; line-height: 1.8; }
								td { padding: 10px 0; }
							</style>
						</head>

						<body >
							<table>
								<tr>
					       			<td >
					           			<img src="https://ticbcs.zfcloud.cc/_nuxt/ticbcsBanner_new.BuPR5fZA.jpg" alt="Conference Banner"  width="640" style="max-width: 100%%; width: 640px; display: block;" object-fit:cover;">
					       			</td>
					   			</tr>
								<tr>
									<td style="text-align: center;font-size:2rem;">您好，感謝您參與此次TICBCS 2025 !</td>
								</tr>
								<tr>
									<td style="text-align: center;" >
										活動當天憑下方QRcode至大會報到處，直接掃描，即可獲得小禮品並快速通關進入會場
									</td>
								</tr>
								<tr>
									<td  style="text-align: center;">
										<img src="https://ticbcs.zfcloud.cc/prod-api/attendees/qrcode?attendeesId=%s" alt="QR Code" />
									</td>
								</tr>
								<tr>
				        			<td  style="text-align: center;">
				            			📍 地點：中國醫藥大學水湳校區 卓越大樓B2 國際會議廳 (406台中市北屯區經貿路一段100號)<br>
				            			📅 時間：2025年06月28日(六) 以及 2025年06月29日(日)
				        			</td>
				    			</tr>
				    			<tr>
				        			<td style="text-align: center;">
				            			若您無法看到 QR Code，請改用 HTML 格式開啟信件，或現場向服務人員出示報名信息。
				        			</td>
				    			</tr>
				    			<tr>
				        			<td style="font-size: 0.9rem; color: #777;">
				        				<br><br><br>
				            			本信件由 TICBCS 大會系統自動發送，請勿直接回信
				        			</td>
				    			</tr>
							</table>
						</body>
					</html>
					"""
				.formatted(checkinRecordVO.getAttendeesVO().getAttendeesId().toString());

		// 8-2.製作純文字信件
		String plainTextContent = """
				您好，感謝您參與此次 TICBCS 2025！

				活動當天請憑下方 QR Code 至大會報到處掃描，即可快速完成報到並領取小禮品。

				此封信件包含您的專屬 QR Code，若您未能看到圖像，請改用 HTML 格式開啟信件，或攜帶此郵件至現場由工作人員協助查詢。

				期待與您現場相見！

				本信件由 TICBCS 大會系統自動發送，請勿直接回信
				""";

		// 8-5.透過異步工作去寄送郵件，因為使用了事務，在事務提交後才執行寄信的異步操作，安全做法
		// 我的是寄信返回的是void ,且沒有在異步任務中呼叫資料庫,所以沒有髒數據問題,所以原本寫法也沒問題
		//		asyncService.sendCommonEmail(member.getEmail(), "【TICBCS 2025 報到確認】現場報到用 QR Code 及活動資訊", htmlContent,
		//				plainTextContent);

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				asyncService.sendCommonEmail(member.getEmail(), "【TICBCS 2025 報到確認】現場報到用 QR Code 及活動資訊", htmlContent,
						plainTextContent);
			}
		});

		// 9.返回簽到的格式
		return checkinRecordVO;

	}

	/**
	 * 用於現場註冊時搭配使用
	 * 
	 * @param member
	 * @return
	 */
	private CheckinRecordVO createAttendeeAndCheckin(Member member) {

		// 1. 建立 Attendee
		AddAttendeesDTO addAttendeesDTO = new AddAttendeesDTO();
		addAttendeesDTO.setEmail(member.getEmail());
		addAttendeesDTO.setMemberId(member.getMemberId());
		Long attendeesId = this.addAfterPayment(addAttendeesDTO);

		// 2. 建立 CheckinRecord
		AddCheckinRecordDTO addCheckinRecordDTO = new AddCheckinRecordDTO();
		addCheckinRecordDTO.setAttendeesId(attendeesId);
		addCheckinRecordDTO.setActionType(CheckinActionTypeEnum.CHECKIN.getValue());
		CheckinRecord checkinRecord = checkinRecordManager.addCheckinRecord(addCheckinRecordDTO);

		// 3. VO 組裝
		AttendeesVO attendeesVO = attendeesManager.getAttendeesVOByAttendeesId(attendeesId);
		CheckinRecordVO checkinRecordVO = checkinRecordConvert.entityToVO(checkinRecord);

		// 4. vo中填入與會者VO對象
		checkinRecordVO.setAttendeesVO(attendeesVO);

		return checkinRecordVO;
	}

	@Override
	public void addAttendees(AddAttendeesDTO addAttendees) {
		// TODO Auto-generated method stub
		// test05
	}

	@Transactional
	@Override
	public Long addAfterPayment(AddAttendeesDTO addAttendees) {

		Attendees attendees = attendeesConvert.addDTOToEntity(addAttendees);
		RLock lock = redissonClient.getLock("attendee:sequence_lock");
		boolean isLocked = false;

		try {
			// 10秒鐘內不斷嘗試獲取鎖，20秒後必定釋放鎖
			isLocked = lock.tryLock(10, 20, TimeUnit.SECONDS);

			if (isLocked) {
				// 鎖內查一次最大 sequence_no
				Integer lockedMax = baseMapper.selectMaxSequenceNo();
				int nextSeq = (lockedMax != null) ? lockedMax + 1 : 1;

				// 如果 設定城當前最大sequence_no
				attendees.setSequenceNo(nextSeq);
				baseMapper.insert(attendees);

				//當前數量，上面已經新增過至少一人，不可能為0
				Long currentCount = baseMapper.selectCount(null);
				// 分組數量
				Integer groupSize = 200;
				// groupIndex組別索引，計算組別 (向上取整，例如 201人 → 第2組)
				Integer groupIndex = (int) Math.ceil(currentCount / (double) groupSize);

				// 獲取或創建Group Tag
				Tag groupTag = tagService.getOrCreateAttendeesGroupTag(groupIndex);

				// 將與會者 與 Tag 做連結
				attendeesTagService.addAttendeesTag(attendees.getAttendeesId(), groupTag.getTagId());

			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (isLocked) {
				lock.unlock();
			}

		}

		// 7.返回主鍵ID
		return attendees.getAttendeesId();

	}

	@Transactional
	@Override
	public void deleteAttendees(Long attendeesId) {

		// 刪除與會者的所有簽到/退紀錄
		checkinRecordManager.deleteCheckinRecordByAttendeesId(attendeesId);

		// 刪除會員在與會者名單的狀態
		baseMapper.deleteById(attendeesId);

	}

	@Override
	public void batchDeleteAttendees(List<Long> attendeesIds) {
		for (Long attendeesId : attendeesIds) {
			this.deleteAttendees(attendeesId);
		}

	}

	@Override
	public void downloadExcel(HttpServletResponse response) throws UnsupportedEncodingException, IOException {
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("與會者名單", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 查詢所有會員，用來填充與會者的基本資訊
		List<Member> memberList = memberManager.getAllMembersEfficiently();

		// 訂單轉成一對一 Map，key為 memberId, value為訂單本身
		Map<Long, Member> memberMap = memberList.stream()
				.collect(Collectors.toMap(Member::getMemberId, Function.identity()));

		// 獲取所有與會者
		List<Attendees> attendeesList = baseMapper.selectAttendees();

		// 資料轉換成Excel
		List<AttendeesExcel> excelData = attendeesList.stream().map(attendees -> {
			AttendeesVO attendeesVO = attendeesConvert.entityToVO(attendees);
			attendeesVO.setMember(memberMap.get(attendees.getMemberId()));
			AttendeesExcel attendeesExcel = attendeesConvert.voToExcel(attendeesVO);

			// 獲取與會者的簡易簽到記錄
			CheckinInfoBO checkinInfoBO = checkinRecordManager
					.getLastCheckinRecordByAttendeesId(attendees.getAttendeesId());
			attendeesExcel.setFirstCheckinTime(checkinInfoBO.getCheckinTime());
			attendeesExcel.setLastCheckoutTime(checkinInfoBO.getCheckoutTime());

			// 匯出專屬簽到/退 QRcode
			try {
				attendeesExcel.setQRcodeImage(
						QrcodeUtil.generateBase64QRCode(attendeesVO.getAttendeesId().toString(), 200, 200));
			} catch (WriterException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return attendeesExcel;

		}).collect(Collectors.toList());

		EasyExcel.write(response.getOutputStream(), AttendeesExcel.class).sheet("與會者列表").doWrite(excelData);

	}

	@Override
	public AttendeesTagVO getAttendeesTagVO(Long attendeesId) {

		// 1.獲取attendees 資料並轉換成 attendeesTagVO
		Attendees attendees = baseMapper.selectById(attendeesId);
		AttendeesTagVO attendeesTagVO = attendeesConvert.entityToAttendeesTagVO(attendees);

		// 2.查詢attendees 的基本資料，並放入Member屬性
		Member member = memberManager.getMemberById(attendees.getMemberId());
		attendeesTagVO.setMember(member);

		// 3.根據 attendeesId 找到與會者所有簽到/退紀錄，並放入CheckinRecord屬性
		List<CheckinRecord> checkinRecordList = checkinRecordManager.getCheckinRecordByAttendeesId(attendeesId);
		attendeesTagVO.setCheckinRecordList(checkinRecordList);

		// 4.isCheckedIn屬性預設是false, 所以只要判斷最新的資料是不是已簽到,如果是再進行更改就好
		CheckinRecord latest = checkinRecordList.stream()
				// ID 為雪花算法，等於時間序
				.max(Comparator.comparing(CheckinRecord::getCheckinRecordId))
				.orElse(null);

		if (latest != null && CheckinActionTypeEnum.CHECKIN.getValue().equals(latest.getActionType())) {
			attendeesTagVO.setIsCheckedIn(true);
		}

		// 5.查詢該attendees所有關聯的tag
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByAttendeesId(attendeesId);

		// 如果沒有任何關聯,就可以直接返回了
		if (attendeesTagList.isEmpty()) {
			return attendeesTagVO;
		}

		// 6.獲取到所有attendeesTag的關聯關係後，提取出tagIds
		List<Long> tagIds = attendeesTagList.stream()
				.map(attendeesTag -> attendeesTag.getTagId())
				.collect(Collectors.toList());

		// 7.去Tag表中查詢實際的Tag資料，並轉換成Set集合
		List<Tag> tagList = tagService.getTagByTagIds(tagIds);
		Set<Tag> tagSet = new HashSet<>(tagList);

		// 8.最後填入attendeesTagVO對象並返回
		attendeesTagVO.setTagSet(tagSet);
		return attendeesTagVO;

	}

	@Override
	public IPage<AttendeesTagVO> getAttendeesTagVOPage(Page<Attendees> pageInfo) {

		// 1.獲得AttendeesPage對象
		IPage<Attendees> attendeesPage = attendeesManager.getAttendeesPage(pageInfo);

		// 2.初始化這兩個數組, 因為是1:1關係，所以size可以直接配初始容量
		List<Long> attendeesIds = new ArrayList<>(attendeesPage.getRecords().size());
		List<Long> memberIds = new ArrayList<>(attendeesPage.getRecords().size());

		// 3.一次遍歷,填充 attendeesId 和 memberId 列表，mybatis plus回傳的List永不為null,就算元素為空也不觸發foreach
		for (Attendees attendee : attendeesPage.getRecords()) {
			attendeesIds.add(attendee.getAttendeesId());
			memberIds.add(attendee.getMemberId());
		}

		// 4.獲取memberMap
		Map<Long, Member> memberMap = memberManager.getMemberMapByIds(memberIds);

		// 5.透過私有方法組裝VO分頁對象
		return this.buildAttendeesTagVOPage(pageInfo, attendeesPage, memberMap);
	}

	@Override
	public IPage<AttendeesTagVO> getAttendeesTagVOPageByQuery(Page<Attendees> pageInfo, String queryText) {

		// 1.與會者沒獨立資訊是給用戶查詢的，所以是查詢會員資訊，queryText都是member的資訊
		List<Member> memberList = memberManager.getMembersByQuery(queryText);

		// 2. 同時建立 memberId → Member 映射，並提取 memberIds
		Map<Long, Member> memberMap = new HashMap<>();
		List<Long> memberIds = new ArrayList<>();
		for (Member member : memberList) {
			memberMap.put(member.getMemberId(), member);
			memberIds.add(member.getMemberId());
		}

		// 3.如果memberIds為空，直接返回一個空Page<AttendeesTagVO>對象
		if (memberIds.isEmpty()) {
			// 直接return 空voPage對象
			IPage<AttendeesTagVO> voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), 0);
			return voPage;
		}

		// 4.如果memberIds不為空，則查詢出符合的attendees (分頁)，並抽取attendeesIds
		IPage<Attendees> attendeesPage = attendeesManager.getAttendeesPageByMemberIds(pageInfo, memberIds);

		// 5.透過私有方法組裝VO分頁對象
		return this.buildAttendeesTagVOPage(pageInfo, attendeesPage, memberMap);

	}

	/**
	 * 組裝AttendeesTagVO
	 * 
	 * @param pageInfo      分頁資訊
	 * @param attendeesPage 與會者分頁對象
	 * @param memberMap     會員ID 與 會員 映射
	 * @return
	 */
	private IPage<AttendeesTagVO> buildAttendeesTagVOPage(Page<?> pageInfo, IPage<Attendees> attendeesPage,
			Map<Long, Member> memberMap) {
		List<Attendees> attendeesList = attendeesPage.getRecords();
		List<Long> attendeesIds = attendeesList.stream().map(Attendees::getAttendeesId).collect(Collectors.toList());

		// 邊界檢查
		if (attendeesIds.isEmpty()) {
			return new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
		}

		// 資料聚合
		Map<Long, List<CheckinRecord>> checkinRecordMap = checkinRecordManager
				.getCheckinMapByAttendeesIds(attendeesIds);
		Map<Long, Boolean> checkinStatusMap = checkinRecordManager.getCheckinStatusMap(checkinRecordMap);
		Map<Long, List<Long>> attendeesTagMap = attendeesTagService.getAttendeesTagMapByAttendeesIds(attendeesIds);
		Map<Long, Tag> tagMap = tagService.getTagMapFromAttendeesTag(attendeesTagMap);

		// 組裝 VO
		List<AttendeesTagVO> voList = attendeesManager.buildAttendeesTagVOList(attendeesList, checkinRecordMap,
				checkinStatusMap, attendeesTagMap, tagMap, memberMap);

		// 回傳分頁物件
		IPage<AttendeesTagVO> voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
		voPage.setRecords(voList);
		return voPage;
	}

	@Override
	@Transactional
	public void assignTagToAttendees(List<Long> targetTagIdList, Long attendeesId) {

		// 1. 查詢當前 attendees 的所有關聯 tag
		List<AttendeesTag> currentAttendeesTags = attendeesTagService.getAttendeesTagByAttendeesId(attendeesId);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentAttendeesTags.stream()
				.map(AttendeesTag::getTagId)
				.collect(Collectors.toSet());

		// 3. 對比目標 attendeesIdList 和當前 attendeesIdList
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
			attendeesTagService.removeTagsFromAttendee(attendeesId, tagsToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			List<AttendeesTag> newAttendeesTags = tagsToAdd.stream().map(tagId -> {
				AttendeesTag attendeesTag = new AttendeesTag();
				attendeesTag.setTagId(tagId);
				attendeesTag.setAttendeesId(attendeesId);
				return attendeesTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (AttendeesTag attendeesTag : newAttendeesTags) {
				attendeesTagService.addAttendeesTag(attendeesTag);
			}
		}
	}

	@Override
	public void sendEmailToAttendeess(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		// 獲取本日預計要寄出的信件量, 為了保證排程任務順利被寄出
		int pendingExpectedEmailVolumeByToday = scheduleEmailTaskService.getPendingExpectedEmailVolumeByToday();

		//初始化 attendeesIdSet ，用於去重attendeesId
		Set<Long> attendeesIdSet = new HashSet<>();

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有會員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的會員人數
		Long attendeesCount = 0L;

		if (hasNoTag) {
			attendeesCount = baseMapper.selectCount(null);
		} else {

			// 拿到與會者ID列表
			attendeesIdSet = this.getAttendeesIdSet(tagIdList);

			if (attendeesIdSet.isEmpty()) {
				throw new EmailException("沒有符合資格的與會者");
			}

			// 如果attendeesIdSet 至少有一個，則開始搜尋Attendees
			LambdaQueryWrapper<Attendees> attendeesWrapper = new LambdaQueryWrapper<>();
			attendeesWrapper.in(Attendees::getAttendeesId, attendeesIdSet);
			attendeesCount = baseMapper.selectCount(attendeesWrapper);

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (attendeesCount <= 0) {
			throw new EmailException("沒有符合資格的與會者");
		} else if (currentQuota - pendingExpectedEmailVolumeByToday < attendeesCount) {
			throw new EmailException("本日寄信額度無法寄送 " + attendeesCount + " 封信");
		}

		// 查收信者名單 + member
		List<AttendeesVO> attendeesVOList = buildAttendeesVOList(hasNoTag ? null : attendeesIdSet);

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		asyncService.batchSendEmail(attendeesVOList, sendEmailDTO, a -> a.getMember().getEmail(),
				this::replaceAttendeesMergeTag);

		// 額度直接扣除 查詢到的會員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-attendeesCount);

	}

	@Override
	public void scheduleEmailToAttendees(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		// 1.拿到與會者ID 列表
		Set<Long> attendeesIdSet = this.getAttendeesIdSet(tagIdList);

		// 2.透過AttendeesID列表拿到 vo列表
		List<AttendeesVO> attendeesVOList = this.buildAttendeesVOList(attendeesIdSet);

		// 3.放入排程任務
		scheduleEmailTaskService.processScheduleEmailTask(sendEmailDTO, attendeesVOList, "attendees",
				a -> a.getMember().getEmail(), this::replaceAttendeesMergeTag);

	}

	/**
	 * 根據 tagIdList 獲取與會者 ID 集合
	 *
	 * @param tagIdList 標籤 ID 列表
	 * @return 與會者 ID 集合，若無標籤或無符合者，則返回空集合
	 */
	private Set<Long> getAttendeesIdSet(List<Long> tagIdList) {
		// 1.若 tagIdList 為空，則表示所有與會者，直接返回 null
		if (tagIdList == null || tagIdList.isEmpty()) {
			return null;
		}

		// 2.透過 tag 找到符合的 attendees 關聯
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByTagIds(tagIdList);

		// 3.從關聯中取出 attendeesId，並使用 Set 去重
		return attendeesTagList.stream().map(AttendeesTag::getAttendeesId).collect(Collectors.toSet());
	}

	/**
	 * 返回 與會者的VO 對象
	 * 
	 * @param attendeesIdSet 與會者的ID集合
	 * @return
	 */
	private List<AttendeesVO> buildAttendeesVOList(Set<Long> attendeesIdSet) {
		List<Attendees> attendeesList;
		if (attendeesIdSet == null || attendeesIdSet.isEmpty()) {
			attendeesList = baseMapper.selectList(null);
		} else {
			LambdaQueryWrapper<Attendees> attendeesWrapper = new LambdaQueryWrapper<>();
			attendeesWrapper.in(Attendees::getAttendeesId, attendeesIdSet);
			attendeesList = baseMapper.selectList(attendeesWrapper);
		}

		// 只查需要的 member
		Set<Long> memberIds = attendeesList.stream()
				.map(Attendees::getMemberId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<Member> memberList = memberManager.getMembersByIds(memberIds);

		Map<Long, Member> memberMap = memberList.stream()
				.collect(Collectors.toMap(Member::getMemberId, Function.identity()));

		// 組裝 VO
		return attendeesList.stream().map(attendees -> {
			AttendeesVO vo = attendeesConvert.entityToVO(attendees);
			vo.setMember(memberMap.get(attendees.getMemberId()));
			return vo;
		}).collect(Collectors.toList());
	}

	public String replaceAttendeesMergeTag(String content, AttendeesVO attendeesVO) {

		String qrCodeUrl = String.format("https://iopbs.org.tw/prod-api/attendees/qrcode?attendeesId=%s",
				attendeesVO.getAttendeesId());

		String newContent = content.replace("{{QRcode}}", "<img src=\"" + qrCodeUrl + "\" alt=\"QR Code\" />")
				.replace("{{name}}", Strings.nullToEmpty(attendeesVO.getMember().getChineseName()));

		return newContent;

	}

}
