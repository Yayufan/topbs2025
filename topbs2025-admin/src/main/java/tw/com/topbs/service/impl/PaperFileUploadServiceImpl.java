package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

	private static final String ABSTRUCTS_PDF = "abstructs_pdf";
	private static final String ABSTRUCTS_DOCX = "abstructs_docx";

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
	public List<PaperFileUpload> getPaperFileUploadListByPaperId(Long paperId) {
		// 找尋稿件的附件列表
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);
		return paperFileUploadList;
	}

	@Override
	public List<PaperFileUpload> getAbstractsByPaperId(Long paperId) {
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId)
				.and(wrapper -> wrapper.eq(PaperFileUpload::getType, ABSTRUCTS_PDF)
						.or()
						.eq(PaperFileUpload::getType, ABSTRUCTS_DOCX));

		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);
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
