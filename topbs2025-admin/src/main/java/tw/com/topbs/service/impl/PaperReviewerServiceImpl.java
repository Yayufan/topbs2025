package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.service.PaperReviewerService;

@Service
@RequiredArgsConstructor
public class PaperReviewerServiceImpl extends ServiceImpl<PaperReviewerMapper, PaperReviewer>
		implements PaperReviewerService {

	private final PaperReviewerConvert paperReviewerConvert;

	@Override
	public PaperReviewer getPaperReviewer(Long paperReviewerId) {
		PaperReviewer paperReviewer = baseMapper.selectById(paperReviewerId);

		return paperReviewer;
	}

	@Override
	public List<PaperReviewer> getPaperReviewerList() {
		List<PaperReviewer> paperReviewerList = baseMapper.selectList(null);
		return paperReviewerList;
	}

	@Override
	public IPage<PaperReviewer> getPaperReviewerPage(Page<PaperReviewer> page) {
		Page<PaperReviewer> paperPage = baseMapper.selectPage(page, null);
		return paperPage;
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
