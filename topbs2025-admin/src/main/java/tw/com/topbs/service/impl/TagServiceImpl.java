package tw.com.topbs.service.impl;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.TagConvert;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutTagDTO;
import tw.com.topbs.pojo.entity.AttendeesTag;
import tw.com.topbs.pojo.entity.MemberTag;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.PaperReviewerTagService;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.TagService;

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

	private final TagConvert tagConvert;
	private final MemberTagService memberTagService;
	private final PaperTagService paperTagService;
	private final PaperReviewerTagService paperReviewerTagService;
	private final AttendeesTagService attendeesTagService;

	@Override
	public List<Tag> getAllTag() {
		List<Tag> tagList = baseMapper.selectList(null);
		return tagList;
	}

	@Override
	public List<Tag> getAllTagByType(String type) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(Tag::getType, type);
		List<Tag> tagList = baseMapper.selectList(tagQueryWrapper);
		return tagList;
	}

	@Override
	public Tag getTagByTypeAndName(String type, String name) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(Tag::getType, type).eq(Tag::getName, name);
		Tag tag = baseMapper.selectOne(tagQueryWrapper);
		return tag;
	}

	@Override
	public List<Tag> getTagByTagIds(Collection<Long> tagIds) {
		LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
		tagWrapper.in(Tag::getTagId, tagIds);
		List<Tag> tagList = baseMapper.selectList(tagWrapper);
		return tagList;
	}

	@Override
	public IPage<Tag> getAllTag(Page<Tag> page) {
		Page<Tag> tagPage = baseMapper.selectPage(page, null);
		return tagPage;
	}

	@Override
	public IPage<Tag> getAllTag(Page<Tag> page, String type) {
		LambdaQueryWrapper<Tag> tagQueryWrapper = new LambdaQueryWrapper<>();
		tagQueryWrapper.eq(StringUtils.isNotEmpty(type), Tag::getType, type);
		Page<Tag> tagPage = baseMapper.selectPage(page, tagQueryWrapper);
		return tagPage;
	}

	@Override
	public Tag getTag(Long tagId) {
		Tag tag = baseMapper.selectById(tagId);
		return tag;
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

	@Transactional
	@Override
	public void assignMemberToTag(List<Long> targetMemberIdList, Long tagId) {
		// 1. 查詢當前 tag 的所有關聯 member
		List<MemberTag> currentMemberTags = memberTagService.getMemberTagByTagId(tagId);

		// 2. 提取當前關聯的 memberId Set
		Set<Long> currentMemberIdSet = currentMemberTags.stream()
				.map(MemberTag::getMemberId)
				.collect(Collectors.toSet());

		// 3. 對比目標 memberIdList 和當前 memberIdList
		Set<Long> targetMemberIdSet = new HashSet<>(targetMemberIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> membersToRemove = new HashSet<>(currentMemberIdSet);
		// 差集：當前有但目標沒有
		membersToRemove.removeAll(targetMemberIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> membersToAdd = new HashSet<>(targetMemberIdSet);
		// 差集：目標有但當前沒有
		membersToAdd.removeAll(currentMemberIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!membersToRemove.isEmpty()) {
			memberTagService.removeMembersFromTag(tagId, membersToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!membersToAdd.isEmpty()) {
			List<MemberTag> newMemberTags = membersToAdd.stream().map(memberId -> {
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
	public void assignPaperToTag(List<Long> targetPaperIdList, Long tagId) {

		// 1. 查詢當前 tag 的所有關聯 paper
		List<PaperTag> currentPaperTags = paperTagService.getPaperTagByTagId(tagId);

		// 2. 提取當前關聯的 paperId Set
		Set<Long> currentPaperIdSet = currentPaperTags.stream().map(PaperTag::getPaperId).collect(Collectors.toSet());

		// 3. 對比目標 paperIdList 和當前 paperIdList
		Set<Long> targetPaperIdSet = new HashSet<>(targetPaperIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> papersToRemove = new HashSet<>(currentPaperIdSet);
		// 差集：當前有但目標沒有
		papersToRemove.removeAll(targetPaperIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> papersToAdd = new HashSet<>(targetPaperIdSet);
		// 差集：目標有但當前沒有
		papersToAdd.removeAll(currentPaperIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!papersToRemove.isEmpty()) {
			paperTagService.removeTagRelationsForPapers(tagId, papersToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!papersToAdd.isEmpty()) {

			List<PaperTag> newPaperTags = papersToAdd.stream().map(paperId -> {
				PaperTag paperTag = new PaperTag();
				paperTag.setTagId(tagId);
				paperTag.setPaperId(paperId);
				return paperTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (PaperTag paperTag : newPaperTags) {
				paperTagService.addPaperTag(paperTag);
			}
		}

	}

	@Override
	public void assignPaperReviewerToTag(List<Long> targetPaperReviewerIdList, Long tagId) {

		// 1. 查詢當前 tag 的所有關聯 paperReviewer
		List<PaperReviewerTag> currentPaperReviewerTags = paperReviewerTagService.getPaperReviewerTagByTagId(tagId);

		// 2. 提取當前關聯的 paperReviewerId Set
		Set<Long> currentPaperReviewerIdSet = currentPaperReviewerTags.stream()
				.map(PaperReviewerTag::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 3. 對比目標 paperReviewerIdList 和當前 paperReviewerIdList
		Set<Long> targetPaperReviewerIdSet = new HashSet<>(targetPaperReviewerIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> paperReviewersToRemove = new HashSet<>(currentPaperReviewerIdSet);
		// 差集：當前有但目標沒有
		paperReviewersToRemove.removeAll(targetPaperReviewerIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> paperReviewersToAdd = new HashSet<>(targetPaperReviewerIdSet);
		// 差集：目標有但當前沒有
		paperReviewersToAdd.removeAll(currentPaperReviewerIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!paperReviewersToRemove.isEmpty()) {
			paperReviewerTagService.removeTagRelationsForPaperReviewers(tagId, paperReviewersToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!paperReviewersToAdd.isEmpty()) {
			List<PaperReviewerTag> newPaperReviewerTags = paperReviewersToAdd.stream().map(paperReviewerId -> {
				PaperReviewerTag paperReviewerTag = new PaperReviewerTag();
				paperReviewerTag.setTagId(tagId);
				paperReviewerTag.setPaperReviewerId(paperReviewerId);
				return paperReviewerTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (PaperReviewerTag paperReviewerTag : newPaperReviewerTags) {
				paperReviewerTagService.addPaperReviewerTag(paperReviewerTag);
			}
		}

	}

	@Override
	public void assignAttendeesToTag(List<Long> targetAttendeesIdList, Long tagId) {

		// 1. 查詢當前 tag 的所有關聯 attendees
		List<AttendeesTag> currentAttendeesTags = attendeesTagService.getAttendeesTagByTagId(tagId);

		// 2. 提取當前關聯的 attendeesId Set
		Set<Long> currentAttendeesIdSet = currentAttendeesTags.stream()
				.map(AttendeesTag::getAttendeesId)
				.collect(Collectors.toSet());

		// 3. 對比目標 attendeesIdList 和當前 attendeesIdList
		Set<Long> targetAttendeesIdSet = new HashSet<>(targetAttendeesIdList);

		// 4. 找出需要 刪除 的關聯關係
		Set<Long> attendeessToRemove = new HashSet<>(currentAttendeesIdSet);
		// 差集：當前有但目標沒有
		attendeessToRemove.removeAll(targetAttendeesIdSet);

		// 5. 找出需要 新增 的關聯關係
		Set<Long> attendeessToAdd = new HashSet<>(targetAttendeesIdSet);
		// 差集：目標有但當前沒有
		attendeessToAdd.removeAll(currentAttendeesIdSet);

		// 6. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!attendeessToRemove.isEmpty()) {
			attendeesTagService.removeAttendeesFromTag(tagId, attendeessToRemove);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!attendeessToAdd.isEmpty()) {
			List<AttendeesTag> newAttendeesTags = attendeessToAdd.stream().map(attendeesId -> {
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

	// 用於計算相似顏色的tag color
	public String adjustColor(String hexColor, int groupIndex, int stepPercent) {
		Color color = Color.decode(hexColor);

		// 轉 HSB (Hue, Saturation, Brightness)
		float[] hsbVals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

		// 增加亮度 (Brightness)
		float newBrightness = Math.min(1.0f, hsbVals[2] + (groupIndex - 1) * (stepPercent / 100f));

		// 轉回 RGB
		int rgb = Color.HSBtoRGB(hsbVals[0], hsbVals[1], newBrightness);

		// 格式化 Hex
		return String.format("#%06X", (0xFFFFFF & rgb));
	}

}
