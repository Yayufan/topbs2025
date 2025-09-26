package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	public List<Tag> getTagByPaperReviewerId(Long paperReviewerId) {

		// 1.查詢當前 paperReviewer 和 tag 的所有關聯 
		LambdaQueryWrapper<PaperReviewerTag> paperReviewerTagWrapper = new LambdaQueryWrapper<>();
		paperReviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId);
		List<PaperReviewerTag> paperReviewerTags = baseMapper.selectList(paperReviewerTagWrapper);

		// 2. 如果完全沒有tag的關聯,則返回一個空數組
		if (paperReviewerTags == null || paperReviewerTags.isEmpty()) {
			return Collections.emptyList();
		}

		// 3. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = paperReviewerTags.stream()
				.map(PaperReviewerTag::getTagId)
				.collect(Collectors.toSet());

		// 4. 根據TagId Set 找到Tag
		LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
		tagWrapper.in(Tag::getTagId, currentTagIdSet);
		List<Tag> tagList = tagMapper.selectList(tagWrapper);

		return tagList;
	}

	@Override
	public Map<Long, List<Tag>> groupTagsByPaperReviewerId(Collection<Long> paperReviewerIds) {
		// 沒有關聯直接返回空映射
		if (paperReviewerIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 1. 查詢所有 paperReviewerTag 關聯
		LambdaQueryWrapper<PaperReviewerTag> paperReviewerTagWrapper = new LambdaQueryWrapper<>();
		paperReviewerTagWrapper.in(PaperReviewerTag::getPaperReviewerId, paperReviewerIds);
		List<PaperReviewerTag> paperReviewerTags = baseMapper.selectList(paperReviewerTagWrapper);

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
	public List<PaperReviewerTag> getPaperReviewerTagByTagId(Long tagId) {
		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperReviewerTag::getTagId, tagId);
		List<PaperReviewerTag> paperReviewerList = baseMapper.selectList(currentQueryWrapper);

		return paperReviewerList;
	}

	@Override
	public List<PaperReviewerTag> getPaperReviewerTagByReviewerIdsAndTagIds(Collection<Long> paperReviewerIds,
			Collection<Long> tagIds) {
		if (paperReviewerIds.isEmpty() || paperReviewerIds.isEmpty()) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		System.out.println("查詢revieweIds 和 tagIds");
		currentQueryWrapper.in(PaperReviewerTag::getPaperReviewerId, paperReviewerIds)
				.in(PaperReviewerTag::getTagId, tagIds);

		return baseMapper.selectList(currentQueryWrapper);

	}

	@Override
	public void addPaperReviewerTag(PaperReviewerTag paperReviewerTag) {
		baseMapper.insert(paperReviewerTag);

	}

	@Override
	public void addPaperReviewerTag(Long paperReviewerId, Long tagId) {

		LambdaQueryWrapper<PaperReviewerTag> reviewerTagWrapper = new LambdaQueryWrapper<>();
		reviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
				.eq(PaperReviewerTag::getTagId, tagId);
		Long count = baseMapper.selectCount(reviewerTagWrapper);

		// 如果Tag有關聯了，就不要在新增了
		if (count >= 1) {
			return;
		}

		// 否則照常新增Tag
		PaperReviewerTag paperReviewerTag = new PaperReviewerTag();
		paperReviewerTag.setPaperReviewerId(paperReviewerId);
		paperReviewerTag.setTagId(tagId);
		baseMapper.insert(paperReviewerTag);

	}

	@Override
	@Transactional
	public void addPaperReviewerTagsBatch(Collection<Long> reviewerIds, Long tagId) {
		if (reviewerIds == null || reviewerIds.isEmpty() || tagId == null) {
			return;
		}

		// 1. 批量查詢這些審稿人是否已經擁有該 Tag
		LambdaQueryWrapper<PaperReviewerTag> existingTagsQuery = new LambdaQueryWrapper<>();
		existingTagsQuery.in(PaperReviewerTag::getPaperReviewerId, reviewerIds);
		existingTagsQuery.eq(PaperReviewerTag::getTagId, tagId);
		List<PaperReviewerTag> existingAssociations = baseMapper.selectList(existingTagsQuery);

		// 提取已經存在關聯的 reviewerId
		Set<Long> existingReviewerIds = existingAssociations.stream()
				.map(PaperReviewerTag::getPaperReviewerId)
				.collect(Collectors.toSet());

		// 2. 篩選出真正需要新增的關聯
		List<PaperReviewerTag> tagsToInsert = new ArrayList<>();
		for (Long reviewerId : reviewerIds) {
			if (!existingReviewerIds.contains(reviewerId)) { // 如果不存在，則添加到新增列表
				PaperReviewerTag newTag = new PaperReviewerTag();
				newTag.setPaperReviewerId(reviewerId);
				newTag.setTagId(tagId);
				// 設置其他必要的屬性，如創建時間等
				tagsToInsert.add(newTag);
			}
		}

		// 3. 執行批量插入
		if (!tagsToInsert.isEmpty()) {
			// 使用 Mybatis-Plus 的 saveBatch 方法進行批量插入
			// 這會將多個 INSERT 語句優化為一條 SQL (或幾條，取決於數據庫和 JDBC 驅動)
			saveBatch(tagsToInsert); // saveBatch 是 ServiceImpl 自帶的方法
		}
	}

	@Override
	public void removePaperReviewerTag(Long paperReviewerId, Long tagId) {
		LambdaQueryWrapper<PaperReviewerTag> reviewerTagWrapper = new LambdaQueryWrapper<>();
		reviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
				.eq(PaperReviewerTag::getTagId, tagId);
		// 直接根據查詢條件刪除
		baseMapper.delete(reviewerTagWrapper);

	}

	@Override
	public void removePaperReviewerTag(Long paperReviewerId, Collection<Long> tagIds) {
		if (tagIds.isEmpty()) {
			return;
		}

		LambdaQueryWrapper<PaperReviewerTag> reviewerTagWrapper = new LambdaQueryWrapper<>();
		reviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
				.in(PaperReviewerTag::getTagId, tagIds);
		// 直接根據查詢條件刪除
		baseMapper.delete(reviewerTagWrapper);

	}

	@Override
	public void removePaperReviewerFromTag(Long tagId, Set<Long> paperReviewersToRemove) {
		// TODO Auto-generated method stub
		LambdaQueryWrapper<PaperReviewerTag> deletePaperReviewerTagWrapper = new LambdaQueryWrapper<>();
		deletePaperReviewerTagWrapper.eq(PaperReviewerTag::getTagId, tagId)
				.in(PaperReviewerTag::getPaperReviewerId, paperReviewersToRemove);
		baseMapper.delete(deletePaperReviewerTagWrapper);

	}

	@Transactional
	@Override
	public void assignTagToPaperReviewer(Long paperReviewerId,List<Long> targetTagIdList) {

		// 1. 查詢當前 paper 的所有關聯 tag
		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId);
		List<PaperReviewerTag> currentPaperReviewerTags = baseMapper.selectList(currentQueryWrapper);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentPaperReviewerTags.stream()
				.map(PaperReviewerTag::getTagId)
				.collect(Collectors.toSet());

		// 3. 對比目標 paperReviewerIdList 和當前 paperReviewerIdList
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
			LambdaQueryWrapper<PaperReviewerTag> deletePaperReviewerTagWrapper = new LambdaQueryWrapper<>();
			deletePaperReviewerTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
					.in(PaperReviewerTag::getTagId, tagsToRemove);
			baseMapper.delete(deletePaperReviewerTagWrapper);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			List<PaperReviewerTag> newPaperReviewerTags = tagsToAdd.stream().map(tagId -> {
				PaperReviewerTag paperTag = new PaperReviewerTag();
				paperTag.setTagId(tagId);
				paperTag.setPaperReviewerId(paperReviewerId);
				return paperTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (PaperReviewerTag paperTag : newPaperReviewerTags) {
				baseMapper.insert(paperTag);
			}
		}

	}

}
