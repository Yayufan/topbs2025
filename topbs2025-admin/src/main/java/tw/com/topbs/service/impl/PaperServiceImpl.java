package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperConvert;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.service.PaperService;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

	private final PaperConvert paperConvert;

	@Override
	public Paper getPaper(Long paperId) {
		Paper paper = baseMapper.selectById(paperId);
		return paper;
	}

	@Override
	public List<Paper> getPaperList() {
		List<Paper> paperList = baseMapper.selectList(null);
		return paperList;
	}

	@Override
	public IPage<Paper> getPaperPage(Page<Paper> page) {
		Page<Paper> paperPage = baseMapper.selectPage(page, null);
		return paperPage;
	}

	@Override
	public void addPaper(AddPaperDTO addPaperDTO) {
		Paper paper = paperConvert.addDTOToEntity(addPaperDTO);
		baseMapper.insert(paper);
		return;
	}

	@Override
	public void updatePaper(PutPaperDTO putPaperDTO) {
		Paper paper = paperConvert.putDTOToEntity(putPaperDTO);
		baseMapper.updateById(paper);

	}

	@Override
	public void deletePaper(Long paperId) {
		baseMapper.deleteById(paperId);

	}

	@Override
	public void deletePaperList(List<Long> paperIds) {
		baseMapper.deleteBatchIds(paperIds);

	}

}
