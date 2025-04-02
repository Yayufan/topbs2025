package tw.com.topbs.service.impl;

import java.util.List;
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
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.service.PaperReviewerService;

@Service
@RequiredArgsConstructor
public class PaperReviewerServiceImpl extends ServiceImpl<PaperReviewerMapper, PaperReviewer>
		implements PaperReviewerService {

	private final PaperReviewerConvert paperReviewerConvert;

	@Override
	public PaperReviewerVO getPaperReviewer(Long paperReviewerId) {
		PaperReviewer paperReviewer = baseMapper.selectById(paperReviewerId);
		PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

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
			return vo;
		}).collect(Collectors.toList());

		return voList;
	}

	@Override
	public IPage<PaperReviewerVO> getPaperReviewerPage(Page<PaperReviewer> page) {
		Page<PaperReviewer> paperPage = baseMapper.selectPage(page, null);
		List<PaperReviewerVO> voList = paperPage.getRecords().stream().map(paperReviewer -> {
			PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);
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

}
