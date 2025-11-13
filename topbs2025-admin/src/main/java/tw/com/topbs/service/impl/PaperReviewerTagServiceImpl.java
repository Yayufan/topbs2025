package tw.com.topbs.service.impl;

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
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.PaperReviewerTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperReviewerTagService;

/**
 * <p>
 * paperReviewer表 和 tag表的關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Service
@RequiredArgsConstructor
public class PaperReviewerTagServiceImpl extends ServiceImpl<PaperReviewerTagMapper, PaperReviewerTag>
		implements PaperReviewerTagService {

	private final TagMapper tagMapper;

	@Override
	public Set<Long> getTagIdsByReviewerId(Long reviewerId) {
		List<PaperReviewerTag> reviewerTags = this.getReviewerTagsByReviewerId(reviewerId);
		return reviewerTags.stream().map(PaperReviewerTag::getTagId).collect(Collectors.toSet());
	}

	@Override
	public List<PaperReviewerTag> getReviewerTagsByReviewerId(Long reviewerId) {
		LambdaQueryWrapper<PaperReviewerTag> paperReviewerTagWrapper = new LambdaQueryWrapper<>();
		paperReviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, reviewerId);
		return baseMapper.selectList(paperReviewerTagWrapper);
	}

	@Override
	public List<PaperReviewerTag> getReviewerTagsByReviewerIds(Collection<Long> reviewerIds) {
		if (reviewerIds.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<PaperReviewerTag> paperReviewerTagWrapper = new LambdaQueryWrapper<>();
		paperReviewerTagWrapper.in(PaperReviewerTag::getPaperReviewerId, reviewerIds);
		return baseMapper.selectList(paperReviewerTagWrapper);
	}

	@Override
	public List<Tag> getTagsByPaperReviewerId(Long reviewerId) {

		// 1.查詢當前 paperReviewer 和 tag 的所有關聯 
		List<PaperReviewerTag> paperReviewerTags = this.getReviewerTagsByReviewerId(reviewerId);

		// 2. 如果完全沒有tag的關聯,則返回一個空數組
		if (paperReviewerTags.isEmpty()) {
			return Collections.emptyList();
		}

		// 3. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = paperReviewerTags.stream()
				.map(PaperReviewerTag::getTagId)
				.collect(Collectors.toSet());

		// 4. 根據TagId Set 找到Tag
		LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
		tagWrapper.in(Tag::getTagId, currentTagIdSet);
		return tagMapper.selectList(tagWrapper);

	}

	@Override
	public Map<Long, List<Tag>> getReviewerTagMapByReviewerId(Collection<Long> paperReviewerIds) {
		// 1. 查詢所有 paperReviewerTag 關聯
		List<PaperReviewerTag> paperReviewerTags = this.getReviewerTagsByReviewerIds(paperReviewerIds);

		// 沒有關聯直接返回空映射
		if (paperReviewerTags.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2. 按 paperReviewerId 分組，收集 tagId
		Map<Long, List<Long>> paperReviewerIdToTagIds = paperReviewerTags.stream()
				.collect(Collectors.groupingBy(PaperReviewerTag::getPaperReviewerId,
						Collectors.mapping(PaperReviewerTag::getTagId, Collectors.toList())));

		// 3. 收集所有 tagId，獲取map中所有value,兩層List(Collection<List<Long>>)要拆開
		Set<Long> allTagIds = paperReviewerIdToTagIds.values()
				.stream()
				.flatMap(List::stream)
				.collect(Collectors.toSet());

		// 4. 批量查詢所有 Tag，並組成映射關係tagId:Tag
		Map<Long, Tag> tagMap = tagMapper.selectBatchIds(allTagIds)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		// 5. 構建最終結果：paperReviewerId -> List<Tag>
		Map<Long, List<Tag>> result = new HashMap<>();

		paperReviewerIdToTagIds.forEach((paperReviewerId, tagIds) -> {
			List<Tag> tags = tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList());
			result.put(paperReviewerId, tags);
		});

		return result;
	}

	@Override
	public Map<Long, List<Tag>> getReviewerTagMapByReviewerId(List<PaperReviewer> reviewerList) {
		Set<Long> reviewerIds = reviewerList.stream()
				.map(PaperReviewer::getPaperReviewerId)
				.collect(Collectors.toSet());

		return this.getReviewerTagMapByReviewerId(reviewerIds);
	}

	@Override
	public List<PaperReviewerTag> getPaperReviewerTagByTagId(Long tagId) {
		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperReviewerTag::getTagId, tagId);
		List<PaperReviewerTag> paperReviewerList = baseMapper.selectList(currentQueryWrapper);

		return paperReviewerList;
	}

	@Override
	public List<PaperReviewerTag> getReviewerTagByReviewerIdsAndTagIds(Collection<Long> reviewerIds,
			Collection<Long> tagIds) {
		if (reviewerIds.isEmpty() || tagIds.isEmpty()) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.in(PaperReviewerTag::getPaperReviewerId, reviewerIds)
				.in(PaperReviewerTag::getTagId, tagIds);

		return baseMapper.selectList(currentQueryWrapper);

	}

	@Override
	public void addPaperReviewerTag(PaperReviewerTag paperReviewerTag) {
		baseMapper.insert(paperReviewerTag);

	}

	@Override
	public void removePaperReviewerTag(Long paperReviewerId, Long tagId) {
		LambdaQueryWrapper<PaperReviewerTag> reviewerTagWrapper = new LambdaQueryWrapper<>();
		reviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
				.eq(PaperReviewerTag::getTagId, tagId);
		// 直接根據查詢條件刪除
		baseMapper.delete(reviewerTagWrapper);

	}

	@Transactional
	@Override
	public void addTagsToReviewer(Long paperReviewerId, Collection<Long> tagsToAdd) {
		// 1.建立多個新連結
		List<PaperReviewerTag> newPaperReviewerTags = tagsToAdd.stream().map(tagId -> {
			PaperReviewerTag paperTag = new PaperReviewerTag();
			paperTag.setTagId(tagId);
			paperTag.setPaperReviewerId(paperReviewerId);
			return paperTag;
		}).collect(Collectors.toList());

		// 2.批量插入
		this.saveBatch(newPaperReviewerTags);
	}

	@Transactional
	@Override
	public void addReviewersToTag(Long tagId, Collection<Long> reviewersToAdd) {
		// 1.建立多個新連結
		List<PaperReviewerTag> newMemberTags = reviewersToAdd.stream().map(reviewerId -> {
			PaperReviewerTag reviewerTag = new PaperReviewerTag();
			reviewerTag.setTagId(tagId);
			reviewerTag.setPaperReviewerId(reviewerId);
			return reviewerTag;
			
		}).collect(Collectors.toList());

		// 2.批量新增
		this.saveBatch(newMemberTags);
	}
	
	@Transactional
	@Override
	public void addUniqueReviewersToTag(Long tagId, Collection<Long> reviewersToAdd) {
		if (reviewersToAdd.isEmpty()) {
	        return;
	    }
	    
	    // 1. 查詢已存在的組合，避免重複新增
	    LambdaQueryWrapper<PaperReviewerTag> existsWrapper = new LambdaQueryWrapper<>();
	    existsWrapper.eq(PaperReviewerTag::getTagId, tagId)
	                .in(PaperReviewerTag::getPaperReviewerId, reviewersToAdd);
	    
	    Set<Long> existingReviewerIds = baseMapper.selectList(existsWrapper)
	            .stream()
	            .map(PaperReviewerTag::getPaperReviewerId)
	            .collect(Collectors.toSet());
	    
	    // 2. 過濾掉已存在的組合
	    List<PaperReviewerTag> newMemberTags = reviewersToAdd.stream()
	            .filter(reviewerId -> !existingReviewerIds.contains(reviewerId))
	            .map(reviewerId -> {
	                PaperReviewerTag reviewerTag = new PaperReviewerTag();
	                reviewerTag.setTagId(tagId);
	                reviewerTag.setPaperReviewerId(reviewerId);
	                return reviewerTag;
	            })
	            .collect(Collectors.toList());
	    
	    // 3. 批量新增（只新增不存在的組合）
	    if (!newMemberTags.isEmpty()) {
	        this.saveBatch(newMemberTags);
	    }
		
	}

	@Transactional
	@Override
	public void removeTagsFromReviewer(Long paperReviewerId, Collection<Long> tagsToRemove) {
		if (tagsToRemove.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<PaperReviewerTag> reviewerTagWrapper = new LambdaQueryWrapper<>();
		reviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
				.in(PaperReviewerTag::getTagId, tagsToRemove);
		// 直接根據查詢條件刪除
		baseMapper.delete(reviewerTagWrapper);

	}

	@Transactional
	@Override
	public void removeReviewersFromTag(Long tagId, Set<Long> reviewersToRemove) {
		if (reviewersToRemove.isEmpty()) {
			return;
		}
		LambdaQueryWrapper<PaperReviewerTag> deletePaperReviewerTagWrapper = new LambdaQueryWrapper<>();
		deletePaperReviewerTagWrapper.eq(PaperReviewerTag::getTagId, tagId)
				.in(PaperReviewerTag::getPaperReviewerId, reviewersToRemove);
		baseMapper.delete(deletePaperReviewerTagWrapper);

	}



}
