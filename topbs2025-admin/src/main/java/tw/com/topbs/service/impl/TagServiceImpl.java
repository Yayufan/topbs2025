package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.TagConvert;
import tw.com.topbs.enums.TagTypeEnum;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutTagDTO;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.TagService;
import tw.com.topbs.strategy.tag.TagStrategy;
import tw.com.topbs.utils.TagColorUtil;

/**
 * <p>
 * 標籤表,用於對Member進行分組 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

	private final Map<String, TagStrategy> strategyMap;
	private final TagConvert tagConvert;

	@Override
	public TagTypeEnum validateAndGetTagType(Collection<Long> tagIds) {
		// 1.判斷tagIds不為空
		if (tagIds.isEmpty()) {
			return null;
		}
		// 2.批量查出所有 Tag
		List<Tag> tags = baseMapper.selectBatchIds(tagIds);
		if (tags.isEmpty()) {
			return null;
		}
		// 3.取得第一個 tag 的 type 作為基準
		String baseType = tags.get(0).getType();

		// 4.檢查是否所有 tag type 都一致
		boolean allSameType = tags.stream().allMatch(tag -> baseType.equals(tag.getType()));
		if (!allSameType) {
			throw new IllegalArgumentException("所有 Tag 的 type 必須一致");
		}

		// 5.以第一個tag type當作typeEnum
		return TagTypeEnum.fromType(baseType);

	}

	private TagStrategy getTagStrategyById(Long tagId) {
		Tag tag = baseMapper.selectById(tagId);
		TagTypeEnum tagTypeEnum = TagTypeEnum.fromType(tag.getType());
		return strategyMap.get(tagTypeEnum.getTagStrategy());
	}

	@Override
	public long countHoldersByTagId(Long tagId) {
		TagStrategy tagStrategy = this.getTagStrategyById(tagId);
		return tagStrategy.countHoldersByTagId(tagId);
	}

	@Override
	public long countHoldersByTagIds(List<Long> tagIds) {
		// 批量查出所有 Tag
		List<Tag> tags = baseMapper.selectBatchIds(tagIds);
		if (tags.isEmpty())
			return 0L;

		// 取得第一個 tag 的 type 作為基準
		String baseType = tags.get(0).getType();

		// 檢查是否所有 tag type 都一致
		boolean allSameType = tags.stream().allMatch(tag -> baseType.equals(tag.getType()));
		if (!allSameType) {
			throw new IllegalArgumentException("所有 Tag 的 type 必須一致");
		}

		// 以第一個tag type拿到策略，並的到去重複的人數
		TagTypeEnum tagTypeEnum = TagTypeEnum.fromType(baseType);
		TagStrategy tagStrategy = strategyMap.get(tagTypeEnum.getTagStrategy());
		return tagStrategy.countHoldersByTagIds(tagIds);

	}

	@Override
	public List<Tag> getTagList() {
		return baseMapper.selectList(null);
	}

	@Override
	public List<Tag> getTagListByType(String type) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(Tag::getType, type);
		return baseMapper.selectList(tagQueryWrapper);
	}

	@Override
	public Tag getTagByTypeAndName(String type, String name) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(Tag::getType, type).eq(Tag::getName, name);
		return baseMapper.selectOne(tagQueryWrapper);
	}

	@Override
	public List<Tag> getTagByTypeAndFuzzyName(String type, String fuzzyName) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(Tag::getType, type).like(Tag::getName, "%" + fuzzyName + "%");
		return baseMapper.selectList(tagQueryWrapper);
	}

	@Override
	public List<Tag> getTagListByIds(Collection<Long> tagIds) {
		if (tagIds.isEmpty()) {
			return Collections.emptyList();
		}
		return baseMapper.selectBatchIds(tagIds);

	}

	@Override
	public IPage<Tag> getTagPage(Page<Tag> page) {
		return baseMapper.selectPage(page, null);
	}

	@Override
	public IPage<Tag> getTagPageByType(Page<Tag> page, String type) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(StringUtils.isNotEmpty(type), Tag::getType, type);
		return baseMapper.selectPage(page, tagQueryWrapper);
	}

	@Override
	public Map<Long, Tag> getTagMapFromAttendeesTag(Map<Long, List<Long>> attendeesTagMap) {
		/**
		 * attendeesTagMap.values() 获取 attendeesTagMap 中所有的值，即 List<Long>
		 * 的集合,長這樣[[],[],[]]。
		 * .stream() 将这个集合转换为一个流（Stream）。
		 * flatMap(Collection::stream) 将每个 List<Long> 转换为一个流，并将所有这些流合并成一个单一的流。,轉換成[]
		 * collect(Collectors.toSet()) 将流中的所有 Tag ID 收集到一个 Set<Long> 中，这样可以确保 Tag ID
		 * 的唯一性。
		 */
		Set<Long> tagIds = attendeesTagMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());

		// 如果tagIds為空,則返回空Map
		if (tagIds.isEmpty())
			return Collections.emptyMap();

		// 返回tagId 和 Tag 的映射
		List<Tag> tags = this.getTagListByIds(new ArrayList<>(tagIds));
		return tags.stream().collect(Collectors.toMap(Tag::getTagId, Function.identity()));
	}

	@Override
	public Tag getTag(Long tagId) {
		return baseMapper.selectById(tagId);
	}

	@Override
	public Long insertTag(AddTagDTO insertTagDTO) {
		Tag tag = tagConvert.addDTOToEntity(insertTagDTO);
		baseMapper.insert(tag);
		return tag.getTagId();
	}

	@Override
	public void updateTag(PutTagDTO updateTagDTO) {
		Tag tag = tagConvert.putDTOToEntity(updateTagDTO);
		baseMapper.updateById(tag);
	}

	@Override
	public void deleteTag(Long tagId) {
		baseMapper.deleteById(tagId);
	}

	// 用於取代get...IdListByTagId
	@Override
	public List<Long> getAssociatedIdsByTagId(Long tagId) {
		TagStrategy tagStrategy = this.getTagStrategyById(tagId);
		return tagStrategy.getAssociatedIdsByTagId(tagId);
	}

	// 用於取代assign...ToTag
	@Override
	public void assignAssociatedToTag(List<Long> targetAssociatedIdList, Long tagId) {
		TagStrategy tagStrategy = this.getTagStrategyById(tagId);
		tagStrategy.assignEntitiesToTag(targetAssociatedIdList, tagId);
	}

	@Override
	public Tag getOrCreateGroupTag(String tagType, String namePrefix, int groupIndex, String baseColor,
			String descriptionTemplate) {
		String tagName = String.format("%s-group-%02d", namePrefix, groupIndex);
		Tag tag = this.getTagByTypeAndName(tagType, tagName);

		if (tag != null) {
			return tag;
		}

		String color = TagColorUtil.adjustColor(baseColor, groupIndex, 5);
		String desc = String.format(descriptionTemplate, groupIndex);
		return this.createTag(tagType, tagName, desc, color);
	}

	@Override
	public Tag getOrCreateMemberGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.MEMBER.getType(), "M", groupIndex, "#4A7056", "會員分組標籤 (第 %d 組)");
	}

	@Override
	public Tag getOrCreateAttendeesGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.ATTENDEES.getType(), "A", groupIndex, "#001F54", "與會者分組標籤 (第 %d 組)");
	}

	@Override
	public Tag getOrCreatePaperGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.PAPER.getType(), "P1", groupIndex, "#9370DB", "稿件分組標籤 (第 %d 組)");
	}

	@Override
	public Tag getOrCreateSecondPaperGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.PAPER.getType(), "P2", groupIndex, "#5E2B97", "二階段稿件分組 (第 %d 組)");
	}

	@Override
	public Tag getOrCreateThirdPaperGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.PAPER.getType(), "P3", groupIndex, "#2e154b", "三階段稿件分組 (第 %d 組)");
	}

	@Override
	public Tag getOrCreateFirstReviewerGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.PAPER_REVIEWER.getType(), "R1", groupIndex, "#A0522D",
				"一階段審稿人分組 (第 %d 組)");
	}

	@Override
	public Tag getOrCreateSecondReviewerGroupTag(int groupIndex) {
		return getOrCreateGroupTag(TagTypeEnum.PAPER_REVIEWER.getType(), "R2", groupIndex, "#65330f",
				"二階段審稿人分組 (第 %d 組)");
	}

	private Tag createTag(String type, String name, String description, String color) {
		Tag tag = new Tag();
		tag.setType(type);
		tag.setName(name);
		tag.setDescription(description);
		tag.setStatus(0);
		tag.setColor(color);
		baseMapper.insert(tag);
		return tag;
	}

}
