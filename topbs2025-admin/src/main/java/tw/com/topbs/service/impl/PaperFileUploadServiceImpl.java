package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperFileUploadConvert;
import tw.com.topbs.enums.PaperFileTypeEnum;
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
	public List<PaperFileUpload> getPaperFileUploadListByPaperId(Long paperId) {
		// 找尋稿件的附件列表
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId);
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);
		return paperFileUploadList;
	}

	@Override
	public List<PaperFileUpload> getPaperFileUploadListByPaperIds(Collection<Long> paperIds) {

		if (paperIds.isEmpty()) {
			return Collections.emptyList();
		}

		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.in(PaperFileUpload::getPaperId, paperIds);
		List<PaperFileUpload> paperFileUploadList = baseMapper.selectList(paperFileUploadWrapper);

		return paperFileUploadList;
	}

	@Override
	public Map<Long, PaperFileUpload> getPaperFileMapByPaperIdAtFirstReviewStage(Collection<Long> paperIds) {
		return this.getPaperFileUploadListByPaperIds(paperIds)
				.stream()
				.filter(paperFileUpload -> PaperFileTypeEnum.ABSTRUCTS_PDF.getValue().equals(paperFileUpload.getType()))
				.collect(Collectors.toMap(PaperFileUpload::getPaperId, Function.identity()));
	}

	@Override
	public Map<Long, List<PaperFileUpload>> groupFileUploadsByPaperId(Collection<Long> paperIds) {
		return this.getPaperFileUploadListByPaperIds(paperIds)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.groupingBy(PaperFileUpload::getPaperId));
	}

	@Override
	public List<PaperFileUpload> getAbstractsByPaperId(Long paperId) {
		LambdaQueryWrapper<PaperFileUpload> paperFileUploadWrapper = new LambdaQueryWrapper<>();
		paperFileUploadWrapper.eq(PaperFileUpload::getPaperId, paperId)
				.and(wrapper -> wrapper.eq(PaperFileUpload::getType, PaperFileTypeEnum.ABSTRUCTS_PDF.getValue())
						.or()
						.eq(PaperFileUpload::getType, PaperFileTypeEnum.ABSTRUCTS_DOCX.getValue()));

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
