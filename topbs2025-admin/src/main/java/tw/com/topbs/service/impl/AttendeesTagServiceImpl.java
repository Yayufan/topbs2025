package tw.com.topbs.service.impl;

import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.mapper.AttendeesTagMapper;
import tw.com.topbs.service.AttendeesTagService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * <p>
 * 與會者 與 標籤 的關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Service
public class AttendeesTagServiceImpl extends ServiceImpl<AttendeesTagMapper, AttendeesTag> implements AttendeesTagService {
	
	@Override
	public List<AttendeesTag> getAttendeesTagByAttendeesId(Long attendeesId) {
		LambdaQueryWrapper<AttendeesTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(AttendeesTag::getAttendeesId, attendeesId);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(currentQueryWrapper);

		return attendeesTagList;
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
	public List<AttendeesTag> getAttendeesTagByTagId(Long tagId) {
		LambdaQueryWrapper<AttendeesTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(AttendeesTag::getTagId, tagId);
		List<AttendeesTag> attendeesTagList = baseMapper.selectList(currentQueryWrapper);

		return attendeesTagList;
	}

	@Override
	public List<AttendeesTag> getAttendeesTagByAttendeesIds(Collection<Long> attendeesIds) {
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
	public void removeAttendeesFromTag(Long tagId, Set<Long> attendeessToRemove) {
		LambdaQueryWrapper<AttendeesTag> deleteAttendeesTagWrapper = new LambdaQueryWrapper<>();
		deleteAttendeesTagWrapper.eq(AttendeesTag::getTagId, tagId)
				.in(AttendeesTag::getAttendeesId, attendeessToRemove);
		baseMapper.delete(deleteAttendeesTagWrapper);

	}

	@Override
	public void removeTagsFromAttendee(Long attendeesId, Set<Long> tagsToRemove) {
		LambdaQueryWrapper<AttendeesTag> deleteAttendeesTagWrapper = new LambdaQueryWrapper<>();
		deleteAttendeesTagWrapper.eq(AttendeesTag::getAttendeesId, attendeesId)
				.in(AttendeesTag::getTagId, tagsToRemove);
		baseMapper.delete(deleteAttendeesTagWrapper);
		
	}
}
