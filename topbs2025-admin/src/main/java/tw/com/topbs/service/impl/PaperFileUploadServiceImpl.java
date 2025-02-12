package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperFileUploadConvert;
import tw.com.topbs.mapper.PaperFileUploadMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperFileUploadDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperFileUploadDTO;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.service.PaperFileUploadService;

@Service
@RequiredArgsConstructor
public class PaperFileUploadServiceImpl extends ServiceImpl<PaperFileUploadMapper, PaperFileUpload>
		implements PaperFileUploadService {

	private final PaperFileUploadConvert paperFileUploadConvert;

	@Override
	public PaperFileUpload getPaperFileUpload(Long paperFileUploadId) {
		PaperFileUpload paperFileUpload = baseMapper.selectById(paperFileUploadId);
		return paperFileUpload;
	}

	@Override
	public List<PaperFileUpload> getPaperFileUploadList() {
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(null);
		return paperFileUploadList;
	}

	@Override
	public IPage<PaperFileUpload> getPaperFileUploadPage(Page<PaperFileUpload> page) {
		Page<PaperFileUpload> paperFileUploadPage = baseMapper.selectPage(page, null);
		return paperFileUploadPage;
	}

	@Override
	public void addPaperFileUpload(AddPaperFileUploadDTO addPaperFileUploadDTO) {
		PaperFileUpload paperFileUpload = paperFileUploadConvert.addDTOToEntity(addPaperFileUploadDTO);
		baseMapper.insert(paperFileUpload);
		return;
	}

	@Override
	public void updatePaperFileUpload(PutPaperFileUploadDTO putPaperFileUploadDTO) {
		PaperFileUpload paperFileUpload = paperFileUploadConvert.putDTOToEntity(putPaperFileUploadDTO);
		baseMapper.updateById(paperFileUpload);

	}

	@Override
	public void deletePaperFileUpload(Long paperFileUploadId) {
		baseMapper.deleteById(paperFileUploadId);

	}

	@Override
	public void deletePaperFileUploadList(List<Long> paperFileUploadIds) {
		baseMapper.deleteBatchIds(paperFileUploadIds);
	}

}
