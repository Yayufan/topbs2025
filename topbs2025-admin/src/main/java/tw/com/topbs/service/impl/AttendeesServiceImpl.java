package tw.com.topbs.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
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

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.zxing.WriterException;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.AttendeesConvert;
import tw.com.topbs.convert.TagConvert;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.manager.AttendeesManager;
import tw.com.topbs.manager.CheckinRecordManager;
import tw.com.topbs.manager.MemberManager;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.pojo.BO.CheckinInfoBO;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddAttendeesDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.pojo.excelPojo.AttendeesExcel;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.TagService;

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
	private final CheckinRecordManager checkinRecordManager;
	private final AttendeesManager attendeesManager;
	private final AttendeesConvert attendeesConvert;
	private final AttendeesTagService attendeesTagService;
	private final TagService tagService;
	private final TagConvert tagConvert;
	private final AsyncService asyncService;

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
	public void addAttendees(AddAttendeesDTO addAttendees) {
		// TODO Auto-generated method stub

	}

	@Transactional
	@Override
	public void addAfterPayment(AddAttendeesDTO addAttendees) {

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

				//每200名與會者(Attendees)設置一個tag, A-group-01, M-group-02(補零兩位數)
				String baseTagName = "A-group-%02d";
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
				String tagType = "attendees";

				// 4. 查詢是否已有該 Tag
				Tag existingTag = tagService.getTagByTypeAndName(tagType, tagName);

				// 5. 如果沒有就創建 Tag
				if (existingTag == null) {
					AddTagDTO addTagDTO = new AddTagDTO();
					addTagDTO.setType(tagType);
					addTagDTO.setName(tagName);
					addTagDTO.setDescription("與會者分組標籤 (第 " + groupIndex + " 組)");
					addTagDTO.setStatus(0);
					String adjustColor = tagService.adjustColor("#001F54", groupIndex, 5);
					addTagDTO.setColor(adjustColor);
					Long insertTagId = tagService.insertTag(addTagDTO);
					Tag currentTag = tagConvert.addDTOToEntity(addTagDTO);
					currentTag.setTagId(insertTagId);
					existingTag = currentTag;
				}

				// 6.透過tagId 去 關聯表 進行關聯新增
				AttendeesTag attendeesTag = new AttendeesTag();
				attendeesTag.setAttendeesId(attendees.getAttendeesId());
				attendeesTag.setTagId(existingTag.getTagId());
				attendeesTagService.addAttendeesTag(attendeesTag);

			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (isLocked) {
				lock.unlock();
			}

		}

	}

	@Override
	public void deleteAttendees(Long attendeesId) {
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

		// 3.查詢該attendees所有關聯的tag
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByAttendeesId(attendeesId);

		// 如果沒有任何關聯,就可以直接返回了
		if (attendeesTagList.isEmpty()) {
			return attendeesTagVO;
		}

		// 4.獲取到所有attendeesTag的關聯關係後，提取出tagIds
		List<Long> tagIds = attendeesTagList.stream()
				.map(attendeesTag -> attendeesTag.getTagId())
				.collect(Collectors.toList());

		// 5.去Tag表中查詢實際的Tag資料，並轉換成Set集合
		List<Tag> tagList = tagService.getTagByTagIds(tagIds);
		Set<Tag> tagSet = new HashSet<>(tagList);

		// 6.最後填入attendeesTagVO對象並返回
		attendeesTagVO.setTagSet(tagSet);
		return attendeesTagVO;

	}

	@Override
	public IPage<AttendeesTagVO> getAttendeesTagVOPage(Page<Attendees> pageInfo) {

		// 1.以attendees當作基底查詢,越新的擺越前面
		LambdaQueryWrapper<Attendees> attendeesWrapper = new LambdaQueryWrapper<>();
		attendeesWrapper.orderByDesc(Attendees::getAttendeesId);

		// 2.查詢 AttendeesPage (分頁)
		IPage<Attendees> attendeesPage = baseMapper.selectPage(pageInfo, attendeesWrapper);

		// 初始化這兩個數組, 因為是1:1關係，所以size可以直接配初始容量
		List<Long> attendeesIds = new ArrayList<>(attendeesPage.getRecords().size());
		List<Long> memberIds = new ArrayList<>(attendeesPage.getRecords().size());

		// 3.一次遍歷,填充 attendeesId 和 memberId 列表，mybatis plus回傳的List永不為null,就算元素為空也不觸發foreach
		for (Attendees attendee : attendeesPage.getRecords()) {
			attendeesIds.add(attendee.getAttendeesId());
			memberIds.add(attendee.getMemberId());
		}

		// 4.先創建要返回的VOPage對象, 最後在塞record即可
		IPage<AttendeesTagVO> voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());

		// 5.如果沒有與會者,也就是資料庫還沒有資料
		if (attendeesIds.isEmpty()) {
			System.out.println("沒有與會者,所以直接返回");
			voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
			voPage.setRecords(Collections.emptyList());
			return voPage;
		}

		// 4. 批量查詢 AttendeesTag 關係表，獲取 attendeesId 对应的 tagId
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByAttendeesIds(attendeesIds);

		//5.先定義attendeesTagMap 和 tagIds 
		Map<Long, List<Long>> attendeesTagMap = new HashMap<>();
		Set<Long> tagIds = new HashSet<>();

		// 6.在一次遍歷中蒐集兩者
		for (AttendeesTag at : attendeesTagList) {
			// 1. 分組：attendeesId → List<tagId>
			/**
			 * 
			 * 如果 attendeesTagMap 中已經存在 at.getAttendeesId() 這個鍵：
			 * 
			 * 直接返回與該鍵關聯的現有 List<Long> (不會創建新的 ArrayList)
			 * Lambda 表達式 k -> new ArrayList<>() 不會被執行
			 * 
			 * 
			 * 如果 attendeesTagMap 中不存在這個鍵：
			 * 
			 * 執行 Lambda 表達式創建新的 ArrayList<>()
			 * 將這個新列表與鍵 at.getAttendeesId() 關聯並存入 attendeesTagMap
			 * 返回這個新列表
			 * 
			 * 
			 * 無論是哪種情況，computeIfAbsent 都會返回一個與該鍵關聯的 List<Long>，然後調用 .add(at.getTagId())
			 * 將標籤ID添加到這個列表中。
			 * 
			 * computeIfAbsent 和後續的 .add() 操作實際上是兩個分開的步驟
			 * 
			 */
			attendeesTagMap.computeIfAbsent(at.getAttendeesId(), k -> new ArrayList<>()).add(at.getTagId());

			// 2. 收集所有 tagId
			tagIds.add(at.getTagId());
		}

		// 7. 批量查 Tag / Member (避免 N+1)
		// Tag Map
		Map<Long, Tag> tagMap = tagIds.isEmpty() ? Collections.emptyMap()
				: tagService.getTagByTagIds(new ArrayList<>(tagIds))
						.stream()
						.collect(Collectors.toMap(Tag::getTagId, tag -> tag));

		// Member Map
		Map<Long, Member> memberMap = memberIds.isEmpty() ? Collections.emptyMap()
				: memberManager.getMembersByIds(new ArrayList<>(memberIds))
						.stream()
						.collect(Collectors.toMap(Member::getMemberId, member -> member));

		// 9. 組裝 VO
		List<AttendeesTagVO> voList = attendeesPage.getRecords().stream().map(attendees -> {
			AttendeesTagVO vo = attendeesConvert.entityToAttendeesTagVO(attendees);

			// 填充 Member
			vo.setMember(memberMap.get(attendees.getMemberId()));

			// 填充 Tags
			List<Long> relatedTagIds = attendeesTagMap.getOrDefault(attendees.getAttendeesId(),
					Collections.emptyList());
			Set<Tag> tagSet = relatedTagIds.stream()
					.map(tagMap::get)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			vo.setTagSet(tagSet);

			return vo;
		}).collect(Collectors.toList());

		// 10. 塞回 VO 分頁
		voPage.setRecords(voList);
		return voPage;

	}

	@Override
	public IPage<AttendeesTagVO> getAttendeesTagVOPageByQuery(Page<Attendees> pageInfo, String queryText) {

		IPage<AttendeesTagVO> voPage;

		// 1.因為能進與會者其實沒有單獨的資訊了，所以是查詢會員資訊，queryText都是member的資訊
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
			voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), 0);
			voPage.setRecords(Collections.emptyList());
			return voPage;
		}

		// 4.如果不為空，則查詢出符合的attendees (分頁)
		LambdaQueryWrapper<Attendees> attendeesWrapper = new LambdaQueryWrapper<>();
		attendeesWrapper.in(Attendees::getMemberId, memberIds);
		Page<Attendees> attendeesPage = baseMapper.selectPage(pageInfo, attendeesWrapper);

		List<Long> attendeesIds = attendeesPage.getRecords()
				.stream()
				.map(Attendees::getAttendeesId)
				.collect(Collectors.toList());

		if (attendeesIds.isEmpty()) {
			voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), 0);
			voPage.setRecords(Collections.emptyList());
			return voPage;
		}

		// 5. 批量查詢 AttendeesTag 關係表，獲取 attendeesId 对应的 tagId
		List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByAttendeesIds(attendeesIds);

		// 6. 將 attendeesId 對應的 tagId 歸類，key 為attendeesId , value 為 tagIdList
		Map<Long, List<Long>> attendeesTagMap = attendeesTagList.stream()
				.collect(Collectors.groupingBy(AttendeesTag::getAttendeesId,
						Collectors.mapping(AttendeesTag::getTagId, Collectors.toList())));

		// 7. 獲取所有 tagId 列表
		List<Long> tagIds = attendeesTagList.stream()
				.map(AttendeesTag::getTagId)
				.distinct()
				.collect(Collectors.toList());

		// 8. 批量查询所有的 Tag，如果關聯的tagIds為空, 那就不用查了，直接返回
		if (tagIds.isEmpty()) {
			System.out.println("沒有任何tag關聯,所以直接返回");
			List<AttendeesTagVO> attendeesTagVOList = attendeesPage.getRecords().stream().map(attendees -> {

				// 轉換成VO對象後，透過map映射找到Member
				AttendeesTagVO vo = attendeesConvert.entityToAttendeesTagVO(attendees);
				Member member = memberMap.get(attendees.getMemberId());
				// 組裝vo後返回
				vo.setMember(member);
				vo.setTagSet(new HashSet<>());
				return vo;
			}).collect(Collectors.toList());
			voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
			voPage.setRecords(attendeesTagVOList);
			return voPage;

		}

		// 定義tagList
		List<Tag> tagList;
		tagList = tagService.getTagByTagIds(tagIds);

		// 9. 將 Tag 按 tagId 歸類
		Map<Long, Tag> tagMap = tagList.stream().collect(Collectors.toMap(Tag::getTagId, tag -> tag));

		// 10. 組裝 VO 數據
		List<AttendeesTagVO> voList = attendeesPage.getRecords().stream().map(attendees -> {

			// 將查找到的Attendees,轉換成VO對象
			AttendeesTagVO vo = attendeesConvert.entityToAttendeesTagVO(attendees);
			// 透過 mapping 找到member, 並組裝進VO
			Member member = memberMap.get(attendees.getMemberId());
			vo.setMember(member);

			// 獲取該 attendeesId 關聯的 tagId 列表
			List<Long> relatedTagIds = attendeesTagMap.getOrDefault(attendees.getAttendeesId(),
					Collections.emptyList());

			// 獲取所有對應的 Tag
			Set<Tag> tagSet = relatedTagIds.stream()
					.map(tagMap::get)
					.filter(Objects::nonNull) // 避免空值
					.collect(Collectors.toSet());

			// 將 tagSet 放入VO中
			vo.setTagSet(tagSet);

			return vo;
		}).collect(Collectors.toList());

		// 10. 重新封装 VO 的分頁對象
		voPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), attendeesPage.getTotal());
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
	public void sendEmailToAttendeess(List<Long> tagIdList, SendEmailDTO sendEmailDTO)
			throws WriterException, IOException {

		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);

		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		//初始化 attendeesIdSet ，用於去重attendeesId
		Set<Long> attendeesIdSet = new HashSet<>();

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有會員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的會員人數
		Long attendeesCount = 0L;

		if (hasNoTag) {
			attendeesCount = baseMapper.selectCount(null);
		} else {
			// 透過tag先找到符合的attendees關聯
			List<AttendeesTag> attendeesTagList = attendeesTagService.getAttendeesTagByTagIds(tagIdList);

			// 從關聯中取出attendeesId ，使用Set去重複的會員，因為會員有可能有多個Tag
			attendeesIdSet = attendeesTagList.stream().map(AttendeesTag::getAttendeesId).collect(Collectors.toSet());

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
		}

		if (currentQuota < attendeesCount) {
			throw new EmailException("本日寄信額度剩餘: " + currentQuota + "，無法寄送 " + attendeesCount + " 封信");
		}

		// 查收信者名單 + member
		List<AttendeesVO> attendeesVOList = buildAttendeesVOList(hasNoTag ? null : attendeesIdSet);

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		asyncService.batchSendEmailToAttendeess(attendeesVOList, sendEmailDTO);

		// 額度直接扣除 查詢到的會員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-attendeesCount);

	}

	// 提取通用代碼製成private function 
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

}
