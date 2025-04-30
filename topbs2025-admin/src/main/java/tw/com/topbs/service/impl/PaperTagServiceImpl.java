package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.PaperTagMapper;
import tw.com.topbs.pojo.entity.PaperTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperTagService;
import tw.com.topbs.service.TagService;

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

	private final TagService tagService;

	@Override
	public List<Tag> getTagByPaperId(Long paperId) {

		// 1. 查詢當前 paper 和 tag 的所有關聯 
		LambdaQueryWrapper<PaperTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.eq(PaperTag::getPaperId, paperId);
		List<PaperTag> paperTags = baseMapper.selectList(paperTagWrapper);

		// 2. 如果完全沒有tag的關聯,則返回一個空數組
		if (paperTags == null || paperTags.isEmpty()) {
			return new ArrayList<>();
		}

		// 3. 提取當前關聯的 tagId Set
		Set<Long> tagIdSet = paperTags.stream().map(PaperTag::getTagId).collect(Collectors.toSet());

		// 4. 根據TagId Set 找到Tag
		List<Tag> tagList = tagService.getTagByTagIdSet(tagIdSet);
		return tagList;

	}

	@Override
	public List<Tag> getPaperByTagId(Long tagId) {
		LambdaQueryWrapper<PaperTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.eq(PaperTag::getTagId, tagId);
		List<PaperTag> paperTags = baseMapper.selectList(paperTagWrapper);
		return null;

	}

}
