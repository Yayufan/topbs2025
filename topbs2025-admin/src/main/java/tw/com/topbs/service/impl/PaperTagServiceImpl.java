package tw.com.topbs.service.impl;

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
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.mapper.PaperTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperTagService;

/**
 * <p>
 * paper表 和 tag表的關聯表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Service
@RequiredArgsConstructor
public class PaperTagServiceImpl extends ServiceImpl<PaperTagMapper, PaperTag> implements PaperTagService {

	private final PaperMapper paperMapper;
	private final TagMapper tagMapper;

	@Override
	public List<Tag> getTagByPaperId(Long paperId) {

		// 1.查詢當前 paper 和 tag 的所有關聯 
		LambdaQueryWrapper<PaperTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.eq(PaperTag::getPaperId, paperId);
		List<PaperTag> paperTags = baseMapper.selectList(paperTagWrapper);

		// 2. 如果完全沒有tag的關聯,則返回一個空數組
		if (paperTags == null || paperTags.isEmpty()) {
			return Collections.emptyList();
		}

		// 3. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = paperTags.stream().map(PaperTag::getTagId).collect(Collectors.toSet());

		// 4. 根據TagId Set 找到Tag
		LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
		tagWrapper.in(Tag::getTagId, currentTagIdSet);
		List<Tag> tagList = tagMapper.selectList(tagWrapper);

		return tagList;

	}

	@Override
	public List<Paper> getPaperByTagId(Long tagId) {
		LambdaQueryWrapper<PaperTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.eq(PaperTag::getTagId, tagId);
		List<PaperTag> paperTags = baseMapper.selectList(paperTagWrapper);

		// 2. 如果完全沒有paper的關聯,則返回一個空數組
		if (paperTags == null || paperTags.isEmpty()) {
			return Collections.emptyList();
		}

		// 3. 提取當前關聯的 paperId Set
		Set<Long> paperIdSet = paperTags.stream().map(PaperTag::getPaperId).collect(Collectors.toSet());

		// 4. 根據PaperId Set 找到Paper
		LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
		paperWrapper.in(Paper::getPaperId, paperIdSet);
		List<Paper> paperList = paperMapper.selectList(paperWrapper);

		return paperList;

	}

	@Override
	public Map<Long, List<Tag>> groupTagsByPaperId(Collection<Long> paperIds) {

		// 沒有關聯直接返回空映射
		if (paperIds.isEmpty()) {
			return Collections.emptyMap();
		}

		// 1. 查詢所有 paperTag 關聯
		LambdaQueryWrapper<PaperTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.in(PaperTag::getPaperId, paperIds);
		List<PaperTag> paperTags = baseMapper.selectList(paperTagWrapper);

		// 沒有關聯直接返回空映射
		if (paperTags.isEmpty()) {
			return Collections.emptyMap();
		}

		// 2. 按 paperId 分組，收集 tagId
		Map<Long, List<Long>> paperIdToTagIds = paperTags.stream()
				.collect(Collectors.groupingBy(PaperTag::getPaperId,
						Collectors.mapping(PaperTag::getTagId, Collectors.toList())));

		// 3. 收集所有 tagId，獲取map中所有value,兩層List(Collection<List<Long>>)要拆開
		Set<Long> allTagIds = paperIdToTagIds.values().stream().flatMap(List::stream).collect(Collectors.toSet());

		// 4. 批量查詢所有 Tag，並組成映射關係tagId:Tag
		Map<Long, Tag> tagMap = tagMapper.selectBatchIds(allTagIds)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(Tag::getTagId, Function.identity()));

		// 5. 構建最終結果：paperId -> List<Tag>
		Map<Long, List<Tag>> result = new HashMap<>();

		paperIdToTagIds.forEach((paperId, tagIds) -> {
			List<Tag> tags = tagIds.stream().map(tagMap::get).filter(Objects::nonNull).collect(Collectors.toList());
			result.put(paperId, tags);
		});

		return result;
	}

	@Override
	public List<PaperTag> getPaperTagByTagId(Long tagId) {
		LambdaQueryWrapper<PaperTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperTag::getTagId, tagId);

		List<PaperTag> paperTagList = baseMapper.selectList(currentQueryWrapper);

		return paperTagList;
	}

	@Override
	public List<PaperTag> getPaperTagBytagIdList(List<Long> tagIdList) {
		LambdaQueryWrapper<PaperTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.in(PaperTag::getTagId, tagIdList);
		List<PaperTag> paperTagList = baseMapper.selectList(paperTagWrapper);
		return paperTagList;
	}

	@Override
	@Transactional
	public void assignTagToPaper(List<Long> targetTagIdList, Long paperId) {

		// 1. 查詢當前 paper 的所有關聯 tag
		LambdaQueryWrapper<PaperTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperTag::getPaperId, paperId);
		List<PaperTag> currentPaperTags = baseMapper.selectList(currentQueryWrapper);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentPaperTags.stream().map(PaperTag::getTagId).collect(Collectors.toSet());

		// 3. 對比目標 paperIdList 和當前 paperIdList
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
			LambdaQueryWrapper<PaperTag> deletePaperTagWrapper = new LambdaQueryWrapper<>();
			deletePaperTagWrapper.eq(PaperTag::getPaperId, paperId).in(PaperTag::getTagId, tagsToRemove);
			baseMapper.delete(deletePaperTagWrapper);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			List<PaperTag> newPaperTags = tagsToAdd.stream().map(tagId -> {
				PaperTag paperTag = new PaperTag();
				paperTag.setTagId(tagId);
				paperTag.setPaperId(paperId);
				return paperTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (PaperTag paperTag : newPaperTags) {
				baseMapper.insert(paperTag);
			}
		}

	}

	@Override
	public void addPaperTag(PaperTag paperTag) {
		baseMapper.insert(paperTag);
	}
	
	@Override
	public void addPaperTag(Long paperId, Long tagId) {
		PaperTag paperTag = new PaperTag();
		paperTag.setPaperId(paperId);
		paperTag.setTagId(tagId);
		baseMapper.insert(paperTag);

	}

	@Override
	public void removePapersFromTag(Long tagId, Collection<Long> papersToRemove) {
		LambdaQueryWrapper<PaperTag> deletePaperTagWrapper = new LambdaQueryWrapper<>();
		deletePaperTagWrapper.eq(PaperTag::getTagId, tagId).in(PaperTag::getPaperId, papersToRemove);
		baseMapper.delete(deletePaperTagWrapper);
	}



	@Override
	public void removeTagsFromMember(Long paperId, Collection<Long> tagsToRemove) {
		LambdaQueryWrapper<PaperTag> deletePaperTagWrapper = new LambdaQueryWrapper<>();
		deletePaperTagWrapper.eq(PaperTag::getPaperId, paperId).in(PaperTag::getTagId, tagsToRemove);
		baseMapper.delete(deletePaperTagWrapper);

	}

}
