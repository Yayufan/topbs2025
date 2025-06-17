package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.AccountPasswordWrongException;
import tw.com.topbs.exception.EmailException;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.mapper.PaperReviewerTagMapper;
import tw.com.topbs.pojo.DTO.PaperReviewerLoginInfo;
import tw.com.topbs.pojo.DTO.PutPaperReviewDTO;
import tw.com.topbs.pojo.DTO.SendEmailDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.VO.ReviewVO;
import tw.com.topbs.pojo.VO.ReviewerScoreStatsVO;
import tw.com.topbs.pojo.entity.PaperAndPaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.pojo.entity.PaperReviewerTag;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.AsyncService;
import tw.com.topbs.service.PaperAndPaperReviewerService;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.service.PaperReviewerTagService;

@Service
@RequiredArgsConstructor
public class PaperReviewerServiceImpl extends ServiceImpl<PaperReviewerMapper, PaperReviewer>
		implements PaperReviewerService {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";
	private static final String REVIEWER_CACHE_INFO_KEY = "paperReviewerInfo";
	private static final String EVENT_TOPIC = "topbs2025";

	private final PaperReviewerConvert paperReviewerConvert;
	private final PaperReviewerTagMapper paperReviewerTagMapper;
	private final PaperReviewerTagService paperReviewerTagService;
	private final PaperReviewerFileService paperReviewerFileService;
	private final PaperAndPaperReviewerService paperAndPaperReviewerService;
	private final AsyncService asyncService;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public PaperReviewerVO getPaperReviewer(Long paperReviewerId) {

		PaperReviewer paperReviewer = baseMapper.selectById(paperReviewerId);
		PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

		// 根據paperReviewerId 獲取Tag
		List<Tag> tagList = paperReviewerTagService.getTagByPaperReviewerId(paperReviewerId);
		vo.setTagList(tagList);

		// 根據paperReviewerId 獲取PaperReviewerFile
		List<PaperReviewerFile> paperReviewerFilesByPaperReviewerId = paperReviewerFileService
				.getPaperReviewerFilesByPaperReviewerId(paperReviewerId);
		vo.setPaperReviewerFileList(paperReviewerFilesByPaperReviewerId);

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
		// 1.查詢所有審稿委員
		List<PaperReviewer> paperReviewerList = baseMapper.selectList(null);

		// 如果沒有元素則直接返回空數組
		if (paperReviewerList.isEmpty()) {
			return Collections.emptyList();
		}

		// 2.透過私有方法拿到vo列表
		List<PaperReviewerVO> paperReviewerVOList = this.buildPaperReviewerVOList(paperReviewerList);

		return paperReviewerVOList;
	}

	@Override
	public IPage<PaperReviewerVO> getPaperReviewerPage(Page<PaperReviewer> page) {
		// 1.獲取分頁對象,提取List
		Page<PaperReviewer> paperReviewerPage = baseMapper.selectPage(page, null);
		List<PaperReviewer> paperReviewerList = paperReviewerPage.getRecords();

		// 2.透過私有方法拿到vo列表
		List<PaperReviewerVO> paperReviewerVOList = this.buildPaperReviewerVOList(paperReviewerList);

		// 3.將結果塞入page對象
		Page<PaperReviewerVO> voPage = new Page<>(paperReviewerPage.getCurrent(), paperReviewerPage.getSize(),
				paperReviewerPage.getTotal());
		voPage.setRecords(paperReviewerVOList);

		return voPage;
	}

	private List<PaperReviewerVO> buildPaperReviewerVOList(List<PaperReviewer> paperReviewerList) {

		// 1.從列表中提取審稿委員ID
		List<Long> paperReviewerIds = paperReviewerList.stream()
				.map(PaperReviewer::getPaperReviewerId)
				.collect(Collectors.toList());

		// 2.獲得審稿委員ID 和 Tag的映射
		Map<Long, List<Tag>> groupTagsByPaperReviewerId = paperReviewerTagService
				.groupTagsByPaperReviewerId(paperReviewerIds);

		Map<Long, List<PaperReviewerFile>> groupFilesByPaperReviewerId = paperReviewerFileService
				.groupFilesByPaperReviewerId(paperReviewerIds);

		// 3.遍歷審稿委員名單，轉換成VO
		List<PaperReviewerVO> voList = paperReviewerList.stream().map(paperReviewer -> {

			PaperReviewerVO vo = paperReviewerConvert.entityToVO(paperReviewer);

			// 根據paperReviewerId 獲取Tag，放入tagList
			vo.setTagList(groupTagsByPaperReviewerId.getOrDefault(paperReviewer.getPaperReviewerId(),
					Collections.emptyList()));

			// 根據paperReviewerId 獲取公文檔案，放入PaperReviewerFileList
			vo.setPaperReviewerFileList(groupFilesByPaperReviewerId.getOrDefault(paperReviewer.getPaperReviewerId(),
					Collections.emptyList()));

			return vo;
		}).collect(Collectors.toList());

		return voList;
	}

	@Override
	public IPage<ReviewerScoreStatsVO> getReviewerScoreStatsVOPage(IPage<ReviewerScoreStatsVO> pageable,
			String reviewStage) {
		return paperAndPaperReviewerService.getReviewerScoreStatsVOPage(pageable, reviewStage);
	}

	@Override
	public void addPaperReviewer(AddPaperReviewerDTO addPaperReviewerDTO) {
		PaperReviewer paperReviewer = paperReviewerConvert.addDTOToEntity(addPaperReviewerDTO);

		// 獲取審稿委員總數
		Long selectCount = baseMapper.selectCount(null);
		Long accountNumber = selectCount + 1;

		// 格式化為 3 位數字，前面補零
		String formattedAccountNumber = String.format("%03d", accountNumber);

		// 自動產生帳號和密碼
		paperReviewer.setAccount(EVENT_TOPIC + formattedAccountNumber);
		paperReviewer.setPassword(paperReviewer.getPhone());

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
		/**
		 * 找到審稿委員所擁有的公文檔案
		 */
		List<PaperReviewerFile> paperReviewerFilesByPaperReviewerId = paperReviewerFileService
				.getPaperReviewerFilesByPaperReviewerId(paperReviewerId);

		/**
		 * 遍歷刪除公文檔案
		 */
		for (PaperReviewerFile paperReviewerFile : paperReviewerFilesByPaperReviewerId) {
			paperReviewerFileService.deletePaperReviewerFile(paperReviewerFile.getPaperReviewerFileId());
		}

		/**
		 * 最後刪除自身資料
		 */
		baseMapper.deleteById(paperReviewerId);
	}

	@Override
	public void deletePaperReviewerList(List<Long> paperReviewerIds) {
		for (Long paperReviewerId : paperReviewerIds) {
			this.deletePaperReviewer(paperReviewerId);
		}
	}

	@Override
	public void assignTagToPaperReviewer(List<Long> targetTagIdList, Long paperReviewerId) {
		paperReviewerTagService.assignTagToPaperReviewer(targetTagIdList, paperReviewerId);
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

	@Override
	public SaTokenInfo login(PaperReviewerLoginInfo paperReviewerLoginInfo) {
		LambdaQueryWrapper<PaperReviewer> paperReviewerWrapper = new LambdaQueryWrapper<>();
		paperReviewerWrapper.eq(PaperReviewer::getAccount, paperReviewerLoginInfo.getAccount())
				.eq(PaperReviewer::getPassword, paperReviewerLoginInfo.getPassword());

		PaperReviewer paperReviewer = baseMapper.selectOne(paperReviewerWrapper);

		if (paperReviewer != null) {
			// 之後應該要以這個會員ID 產生Token 回傳前端，讓他直接進入登入狀態
			StpKit.PAPER_REVIEWER.login(paperReviewer.getPaperReviewerId());

			// 登入後才能取得session
			SaSession session = StpKit.PAPER_REVIEWER.getSession();
			// 並對此token 設置會員的緩存資料
			session.set(REVIEWER_CACHE_INFO_KEY, paperReviewer);
			SaTokenInfo tokenInfo = StpKit.PAPER_REVIEWER.getTokenInfo();

			return tokenInfo;
		}

		// 如果 paperReviewer為null , 則直接拋出異常
		throw new AccountPasswordWrongException("Wrong account or password");
	}

	@Override
	public void logout() {
		// 根據token 直接做登出
		StpKit.PAPER_REVIEWER.logout();

	}

	@Override
	public PaperReviewer getPaperReviewerInfo() {
		// 審稿委員登入後才能取得session
		SaSession session = StpKit.PAPER_REVIEWER.getSession();
		// 獲取當前使用者的資料
		PaperReviewer paperReviewerInfo = (PaperReviewer) session.get(REVIEWER_CACHE_INFO_KEY);
		return paperReviewerInfo;
	}

	@Override
	public IPage<ReviewVO> getReviewVOPageByReviewerIdAndReviewStage(IPage<PaperAndPaperReviewer> pageable,
			Long reviewerId, String reviewStage) {

		// 初始化Page對象
		IPage<ReviewVO> reviewVOPage = new Page<>();

		// 如果reviewStage 為第一階段
		if (ReviewStageEnum.FIRST_REVIEW.getValue().equals(reviewStage)) {
			reviewVOPage = paperAndPaperReviewerService.getReviewVOPageByReviewerIdAtFirstReview(pageable, reviewerId);

			// 如果reviewStage 為第二階段
		} else if (ReviewStageEnum.SECOND_REVIEW.getValue().equals(reviewStage)) {
			reviewVOPage = paperAndPaperReviewerService.getReviewVOPageByReviewerIdAtSecondReview(pageable, reviewerId);
		}

		return reviewVOPage;
	}

	@Override
	public void submitReviewScore(PutPaperReviewDTO putPaperReviewDTO) {
		// 調用關聯表方法去修改審核分數
		paperAndPaperReviewerService.submitReviewScore(putPaperReviewDTO);
	}

}
