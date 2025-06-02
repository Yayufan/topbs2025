package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.mapper.PaperReviewerTagMapper;
import tw.com.topbs.mapper.TagMapper;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperReviewerService;

@Service
@RequiredArgsConstructor
public class PaperReviewerServiceImpl extends ServiceImpl<PaperReviewerMapper, PaperReviewer>
		implements PaperReviewerService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";

	private final PaperReviewerConvert paperReviewerConvert;
	private final PaperReviewerTagMapper paperReviewerTagMapper;
	private final TagMapper tagMapper;
	private final AsyncService asyncService;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

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

	@Override
	public void sendEmailToPaperReviewers(List<Long> tagIdList, SendEmailDTO sendEmailDTO) {

		//從Redis中查看本日信件餘額
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		long currentQuota = quota.get();

		// 如果信件額度 小於等於 0，直接返回錯誤不要寄信
		if (currentQuota <= 0) {
			throw new EmailException("今日寄信配額已用完");
		}

		// 先判斷tagIdList是否為空數組 或者 null ，如果true 則是要寄給所有審稿委員
		Boolean hasNoTag = tagIdList == null || tagIdList.isEmpty();

		//初始化要寄信的審稿委員人數
		Long paperReviewerCount = 0L;

		//初始化要寄信的審稿委員
		List<PaperReviewer> paperReviewerList = new ArrayList<>();

		//初始化 paperReviewerIdSet ，用於去重paperReviewerId
		Set<Long> paperReviewerIdSet = new HashSet<>();

		if (hasNoTag) {
			paperReviewerCount = baseMapper.selectCount(null);
		} else {
			// 透過tag先找到符合的paperReviewer關聯
			LambdaQueryWrapper<PaperReviewerTag> paperReviewerTagWrapper = new LambdaQueryWrapper<>();
			paperReviewerTagWrapper.in(PaperReviewerTag::getTagId, tagIdList);
			List<PaperReviewerTag> paperReviewerTagList = paperReviewerTagMapper.selectList(paperReviewerTagWrapper);

			// 從關聯中取出paperReviewerId ，使用Set去重複的審稿委員，因為審稿委員有可能有多個Tag
			paperReviewerIdSet = paperReviewerTagList.stream()
					.map(paperReviewerTag -> paperReviewerTag.getPaperReviewerId())
					.collect(Collectors.toSet());

			// 如果paperReviewerIdSet 至少有一個，則開始搜尋PaperReviewer
			if (!paperReviewerIdSet.isEmpty()) {
				LambdaQueryWrapper<PaperReviewer> paperReviewerWrapper = new LambdaQueryWrapper<>();
				paperReviewerWrapper.in(PaperReviewer::getPaperReviewerId, paperReviewerIdSet);
				paperReviewerCount = baseMapper.selectCount(paperReviewerWrapper);
			}

		}

		//這邊都先排除沒信件額度，和沒有收信者的情況
		if (currentQuota < paperReviewerCount) {
			throw new EmailException("本日寄信額度剩餘: " + currentQuota + "，無法寄送 " + paperReviewerCount + " 封信");
		} else if (paperReviewerCount <= 0) {
			throw new EmailException("沒有符合資格的審稿委員");
		}

		// 前面都已經透過總數先排除了 額度不足、沒有符合資格審稿委員的狀況，現在實際來獲取收信者名單
		// 沒有篩選任何Tag的，則給他所有PaperReviewer名單
		if (hasNoTag) {
			paperReviewerList = baseMapper.selectList(null);
		} else {
			// 如果paperReviewerIdSet 至少有一個，則開始搜尋PaperReviewer
			if (!paperReviewerIdSet.isEmpty()) {
				LambdaQueryWrapper<PaperReviewer> paperReviewerWrapper = new LambdaQueryWrapper<>();
				paperReviewerWrapper.in(PaperReviewer::getPaperReviewerId, paperReviewerIdSet);
				paperReviewerList = baseMapper.selectList(paperReviewerWrapper);
			}

		}

		//前面已排除null 和 0 的狀況，開 異步線程 直接開始遍歷寄信
		asyncService.batchSendEmailToPaperReviewer(paperReviewerList, sendEmailDTO);

		// 額度直接扣除 查詢到的審稿委員數量
		// 避免多用戶操作時，明明已經達到寄信額度，但異步線程仍未扣除完成
		quota.addAndGet(-paperReviewerCount);

	}



}
