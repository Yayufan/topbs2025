package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.AttendeesMapper;
import tw.com.topbs.mapper.AttendeesTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AttendeesTagService;

/**
 * <p>
 * 與會者 與 標籤 的關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Service
@RequiredArgsConstructor
public class AttendeesTagServiceImpl extends ServiceImpl<AttendeesTagMapper, AttendeesTag>
		implements AttendeesTagService {

	private final AttendeesMapper attendeesMapper;
	private final TagMapper tagMapper;

	@Override
	public Set<Long> getTagIdsByAttendeesId(Long attendeesId) {
		// 1.透過attendeesId 找到attendees 與 tag 的關聯
		LambdaQueryWrapper<AttendeesTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(AttendeesTag::getAttendeesId, attendeesId);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(currentQueryWrapper);

		// 2.透過stream流抽取tagId, 變成Set集合
		return attendeesTagList.stream().map(attendeesTag -> attendeesTag.getTagId()).collect(Collectors.toSet());

	}

	@Override
	public List<AttendeesTag> getAttendeesTagByAttendeesId(Long attendeesId) {
		LambdaQueryWrapper<AttendeesTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(AttendeesTag::getAttendeesId, attendeesId);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(currentQueryWrapper);

		return attendeesTagList;
	}

	@Override
	public List<Tag> getTagsByAttendeesId(Long attendeesId) {
		// 1.查找關聯,提取tagIds
		List<AttendeesTag> attendeesTags = this.getAttendeesTagByAttendeesId(attendeesId);
		Set<Long> tagIds = attendeesTags.stream().map(AttendeesTag::getTagId).collect(Collectors.toSet());

		// 2.沒有返回空陣列
		if (tagIds.isEmpty()) {
			Collections.emptyList();
		}

		// 3.查找持有的tag
		LambdaQueryWrapper<Tag> tagyWrapper = new LambdaQueryWrapper<>();
		tagyWrapper.in(Tag::getTagId, tagIds);
		return tagMapper.selectList(tagyWrapper);

	}

	@Override
	public Map<Long, List<Long>> getAttendeesTagMapByAttendeesIds(Collection<Long> attendeesIds) {
		// 先獲取所有關聯關係
		List<AttendeesTag> tagList = this.getAttendeesTagByAttendeesIds(attendeesIds);
		// 設立結果集用來儲存
		Map<Long, List<Long>> result = new HashMap<>();

		// 將所有關係進行遍歷
		for (AttendeesTag at : tagList) {
			// 1. 分組：attendeesId → List<tagId>
			/**
			 * 
			 * 如果 result 中已經存在 at.getAttendeesId() 這個鍵：
			 * 
			 * 直接返回與該鍵關聯的現有 List<Long> (不會創建新的 ArrayList)
			 * Lambda 表達式 k -> new ArrayList<>() 不會被執行
			 * 
			 * 
			 * 如果 result 中不存在這個鍵：
			 * 
			 * 執行 Lambda 表達式創建新的 ArrayList<>()
			 * 將這個新列表與鍵 at.getAttendeesId() 關聯並存入 result
			 * 返回這個新列表
			 * 
			 * 
			 * 無論是哪種情況，computeIfAbsent 都會返回一個與該鍵關聯的 List<Long>，然後調用 .add(at.getTagId())
			 * 將標籤ID添加到這個列表中。
			 * 
			 * computeIfAbsent 和後續的 .add() 操作實際上是兩個分開的步驟
			 * 
			 */
			result.computeIfAbsent(at.getAttendeesId(), k -> new ArrayList<>()).add(at.getTagId());
		}
		return result;
	}

	@Override
	public Map<Long, List<Tag>> getTagMapByAttendeesId(Collection<Attendees> attendeesList) {
		// 1.將attendeesList提取attendeesId ，獲取所有關聯
		Set<Long> attnedeesIdSet = attendeesList.stream().map(Attendees::getAttendeesId).collect(Collectors.toSet());
		List<AttendeesTag> attendeesTags = this.getAttendeesTagByAttendeesIds(attnedeesIdSet);

		// 2. 按 attendeesId 分組，收集 tagId
		Map<Long, List<Long>> attendeesIdToTagIds = attendeesTags.stream()
				.collect(Collectors.groupingBy(AttendeesTag::getAttendeesId,
						Collectors.mapping(AttendeesTag::getTagId, Collectors.toList())));

		// 3. 收集所有 tagId，獲取map中所有value,兩層List(Collection<List<Long>>)要拆開
		Set<Long> allTagIds = attendeesIdToTagIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());

		// 4. 批量查詢所有 Tag，並組成映射關係tagId:Tag
		Map<Long, Tag> tagMap = tagMapper.selectBatchIds(allTagIds)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		// 5. 構建最終結果：attendeesId -> List<Tag>
		Map<Long, List<Tag>> result = new HashMap<>();

		attendeesIdToTagIds.forEach((attendeesId, tagIds) -> {
			List<Tag> tags = tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList());
			result.put(attendeesId, tags);
		});

		return result;

	}

	@Override
	public List<AttendeesTag> getAttendeesTagByTagId(Long tagId) {
		LambdaQueryWrapper<AttendeesTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(AttendeesTag::getTagId, tagId);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(currentQueryWrapper);

		return attendeesTagList;
	}

	@Override
	public List<AttendeesTag> getAttendeesTagByAttendeesIds(Collection<Long> attendeesIds) {
		if (attendeesIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<AttendeesTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.in(AttendeesTag::getAttendeesId, attendeesIds);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(currentQueryWrapper);

		return attendeesTagList;
	}

	@Override
	public List<AttendeesTag> getAttendeesTagByTagIds(Collection<Long> tagIds) {
		LambdaQueryWrapper<AttendeesTag> attendeesTagWrapper = new LambdaQueryWrapper<>();
		attendeesTagWrapper.in(AttendeesTag::getTagId, tagIds);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(attendeesTagWrapper);

		return attendeesTagList;
	}

	@Override
	public void addAttendeesTag(AttendeesTag attendeesTag) {
		baseMapper.insert(attendeesTag);

	}

	@Override
	public void addAttendeesTag(Long attendeesId, Long tagId) {
		AttendeesTag attendeesTag = new AttendeesTag();
		attendeesTag.setAttendeesId(attendeesId);
		attendeesTag.setTagId(tagId);
		baseMapper.insert(attendeesTag);
	}

	@Override
	public void addTagsToAttendees(Long attendeesId, Collection<Long> tagsToAdd) {
		// 1.建立多個新連結
		List<AttendeesTag> newAttendeesTags = tagsToAdd.stream().map(tagId -> {
			AttendeesTag attendeesTag = new AttendeesTag();
			attendeesTag.setTagId(tagId);
			attendeesTag.setAttendeesId(attendeesId);
			return attendeesTag;
		}).collect(Collectors.toList());

		// 2.批量新增
		this.saveBatch(newAttendeesTags);
	}


	@Override
	public void removeTagsFromAttendee(Long attendeesId, Collection<Long> tagsToRemove) {
		LambdaQueryWrapper<AttendeesTag> deleteAttendeesTagWrapper = new LambdaQueryWrapper<>();
		deleteAttendeesTagWrapper.eq(AttendeesTag::getAttendeesId, attendeesId)
				.in(AttendeesTag::getTagId, tagsToRemove);
		baseMapper.delete(deleteAttendeesTagWrapper);

	}
	
	@Override
	public void removeAttendeesFromTag(Long tagId, Set<Long> attendeessToRemove) {
		LambdaQueryWrapper<AttendeesTag> deleteAttendeesTagWrapper = new LambdaQueryWrapper<>();
		deleteAttendeesTagWrapper.eq(AttendeesTag::getTagId, tagId)
				.in(AttendeesTag::getAttendeesId, attendeessToRemove);
		baseMapper.delete(deleteAttendeesTagWrapper);

	}

}
