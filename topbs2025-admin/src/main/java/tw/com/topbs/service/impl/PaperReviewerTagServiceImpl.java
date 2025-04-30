package tw.com.topbs.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.mapper.PaperReviewerTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
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

	private final PaperReviewerMapper paperReviewerMapper;
	private final TagMapper tagMapper;

	@Override
	public List<PaperReviewerTag> getPaperReviewerTagByTagId(Long tagId) {
		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperReviewerTag::getTagId, tagId);
		List<PaperReviewerTag> paperReviewerList = baseMapper.selectList(currentQueryWrapper);

		return paperReviewerList;
	}

	@Override
	public void addPaperReviewerTag(PaperReviewerTag paperReviewerTag) {
		baseMapper.insert(paperReviewerTag);

	}

	@Override
	public void removeTagRelationsForPaperReviewers(Long tagId, Set<Long> paperReviewersToRemove) {
		// TODO Auto-generated method stub
		LambdaQueryWrapper<PaperReviewerTag> deletePaperReviewerTagWrapper = new LambdaQueryWrapper<>();
		deletePaperReviewerTagWrapper.eq(PaperReviewerTag::getTagId, tagId)
				.in(PaperReviewerTag::getPaperReviewerId, paperReviewersToRemove);
		baseMapper.delete(deletePaperReviewerTagWrapper);

	}

}
