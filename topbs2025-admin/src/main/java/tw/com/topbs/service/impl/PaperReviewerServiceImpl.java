package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.mapper.PaperReviewerTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.PaperReviewerService;

@Service
@RequiredArgsConstructor
public class PaperReviewerServiceImpl extends ServiceImpl<PaperReviewerMapper, PaperReviewer>
		implements PaperReviewerService {

	private final PaperReviewerConvert paperReviewerConvert;
	private final PaperReviewerTagMapper paperReviewerTagMapper;
	private final TagMapper tagMapper;

	@Override
	public PaperReviewerVO getPaperReviewer(Long paperReviewerId) {
		PaperReviewer paperReviewer = baseMapper.selectById(paperReviewerId);
		PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

		// 根據paperReviewerId 獲取Tag
		List<Tag> tagList = this.getTagByPaperReviewerId(paperReviewerId);
		vo.setTagList(tagList);

		return vo;
	}

	@Override
	public List<PaperReviewer> getPaperReviewerListByAbsType(String absType) {
		LambdaQueryWrapper<PaperReviewer> paperReviewerWrapper = new LambdaQueryWrapper<>();
		paperReviewerWrapper.like(StringUtils.isNotBlank(absType), PaperReviewer::getAbsTypeList, absType);

		List<PaperReviewer> paperReviewerList = baseMapper.selectList(paperReviewerWrapper);

		return paperReviewerList;
	}

	@Override
	public List<PaperReviewerVO> getPaperReviewerList() {
		List<PaperReviewer> paperReviewerList = baseMapper.selectList(null);

		List<PaperReviewerVO> voList = paperReviewerList.stream().map(paperReviewer -> {
			PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

			// 根據paperReviewerId 獲取Tag
			List<Tag> tagList = this.getTagByPaperReviewerId(paperReviewer.getPaperReviewerId());
			vo.setTagList(tagList);

			return vo;
		}).collect(Collectors.toList());

		return voList;
	}

	@Override
	public IPage<PaperReviewerVO> getPaperReviewerPage(Page<PaperReviewer> page) {
		Page<PaperReviewer> paperPage = baseMapper.selectPage(page, null);
		List<PaperReviewerVO> voList = paperPage.getRecords().stream().map(paperReviewer -> {
			PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

			// 根據paperReviewerId 獲取Tag
			List<Tag> tagList = this.getTagByPaperReviewerId(paperReviewer.getPaperReviewerId());
			vo.setTagList(tagList);

			return vo;
		}).collect(Collectors.toList());
		Page<PaperReviewerVO> voPage = new Page<>(paperPage.getCurrent(), paperPage.getSize(), paperPage.getTotal());
		voPage.setRecords(voList);
		return voPage;
	}

	@Override
	public void addPaperReviewer(AddPaperReviewerDTO addPaperReviewerDTO) {
		PaperReviewer paperReviewer = paperReviewerConvert.addDTOToEntity(addPaperReviewerDTO);
		baseMapper.insert(paperReviewer);
		return;
	}

	@Override
	public void updatePaperReviewer(PutPaperReviewerDTO putPaperReviewerDTO) {
		PaperReviewer paperReviewer = paperReviewerConvert.putDTOToEntity(putPaperReviewerDTO);
		baseMapper.updateById(paperReviewer);

	}

	@Override
	public void deletePaperReviewer(Long paperReviewerId) {
		baseMapper.deleteById(paperReviewerId);
	}

	@Override
	public void deletePaperReviewerList(List<Long> paperReviewerIds) {
		baseMapper.deleteBatchIds(paperReviewerIds);
	}

	@Override
	public void assignTagToPaperReviewer(List<Long> targetTagIdList, Long paperReviewerId) {
		// 1. 查詢當前 paperReviewer 的所有關聯 tag
		LambdaQueryWrapper<PaperReviewerTag> currentQueryWrapper = new LambdaQueryWrapper<>();
		currentQueryWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId);
		List<PaperReviewerTag> currentPaperReviewerTags = paperReviewerTagMapper.selectList(currentQueryWrapper);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentPaperReviewerTags.stream()
				.map(PaperReviewerTag::getTagId)
				.collect(Collectors.toSet());

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
			LambdaQueryWrapper<PaperReviewerTag> deletePaperTagWrapper = new LambdaQueryWrapper<>();
			deletePaperTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId)
					.in(PaperReviewerTag::getTagId, tagsToRemove);
			paperReviewerTagMapper.delete(deletePaperTagWrapper);
		}

		// 7. 執行新增操作，如果 需新增集合 中不為空，則開始新增
		if (!tagsToAdd.isEmpty()) {
			List<PaperReviewerTag> newPaperReviewerTags = tagsToAdd.stream().map(tagId -> {
				PaperReviewerTag newPaperReviewerTag = new PaperReviewerTag();
				newPaperReviewerTag.setTagId(tagId);
				newPaperReviewerTag.setPaperReviewerId(paperReviewerId);
				return newPaperReviewerTag;
			}).collect(Collectors.toList());

			// 批量插入
			for (PaperReviewerTag paperReviewerTag : newPaperReviewerTags) {
				paperReviewerTagMapper.insert(paperReviewerTag);
			}
		}
	}

	private List<Tag> getTagByPaperReviewerId(Long paperReviewerId) {
		// 1. 查詢當前 paper 和 tag 的所有關聯 
		LambdaQueryWrapper<PaperReviewerTag> paperTagWrapper = new LambdaQueryWrapper<>();
		paperTagWrapper.eq(PaperReviewerTag::getPaperReviewerId, paperReviewerId);
		List<PaperReviewerTag> currentPaperTags = paperReviewerTagMapper.selectList(paperTagWrapper);

		// 2. 提取當前關聯的 tagId Set
		Set<Long> currentTagIdSet = currentPaperTags.stream()
				.map(PaperReviewerTag::getTagId)
				.collect(Collectors.toSet());

		if (currentPaperTags.isEmpty()) {
			return new ArrayList<>();
		} else {
			// 3. 根據TagId Set 找到Tag
			LambdaQueryWrapper<Tag> tagWrapper = new LambdaQueryWrapper<>();
			tagWrapper.in(!currentTagIdSet.isEmpty(), Tag::getTagId, currentTagIdSet);
			List<Tag> tagList = tagMapper.selectList(tagWrapper);

			return tagList;
		}

	}

}
