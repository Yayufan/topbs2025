package tw.com.topbs.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.constant.I18nMessageKey;
import tw.com.topbs.convert.PaperReviewerConvert;
import tw.com.topbs.exception.AccountPasswordWrongException;
import tw.com.topbs.helper.MessageHelper;
import tw.com.topbs.mapper.PaperReviewerMapper;
import tw.com.topbs.pojo.DTO.PaperReviewerLoginInfo;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.pojo.entity.PaperReviewerFile;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.PaperReviewerFileService;
import tw.com.topbs.service.PaperReviewerService;

@Service
@RequiredArgsConstructor
public class PaperReviewerServiceImpl extends ServiceImpl<PaperReviewerMapper, PaperReviewer>
		implements PaperReviewerService {

	private static final String REVIEWER_CACHE_INFO_KEY = "paperReviewerInfo";
	
	@Value("${project.name}")
	private String PROJECT_NAME;
	
	@Value("${project.alias}")
	private String ALIAS ;
	

	private final MessageHelper messageHelper;
	private final PaperReviewerConvert paperReviewerConvert;
	private final PaperReviewerFileService paperReviewerFileService;

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	@Override
	public long getReviewerCount() {
		return baseMapper.selectCount(null);
	}

	@Override
	public int getReviewerGroupIndex(int groupSize) {
		long reviewerCount = this.getReviewerCount();
		return (int) Math.ceil(reviewerCount / (double) groupSize);
	}

	@Override
	public PaperReviewer getReviewerById(Long reviewerId) {
		return baseMapper.selectById(reviewerId);
	}

	@Override
	public List<PaperReviewer> getReviewersEfficiently() {
		return baseMapper.selectReviewers();
	}

	@Override
	public List<PaperReviewer> getReviewerListByAbsType(String absType) {
		LambdaQueryWrapper<PaperReviewer> paperReviewerWrapper = new LambdaQueryWrapper<>();
		paperReviewerWrapper.like(PaperReviewer::getAbsTypeList, absType);
		return baseMapper.selectList(paperReviewerWrapper);
	}

	@Override
	public List<PaperReviewer> getReviewerListByIds(Collection<Long> reviewerIds) {
		return baseMapper.selectBatchIds(reviewerIds);
	}

	@Override
	public IPage<PaperReviewer> getReviewerPage(Page<PaperReviewer> page) {
		return baseMapper.selectPage(page, null);
	}

	@Override
	public Map<Long, PaperReviewer> getReviewerMapById(Collection<Long> reviewerIds) {
		List<PaperReviewer> reviewerList = this.getReviewerListByIds(reviewerIds);
		return reviewerList.stream().collect(Collectors.toMap(PaperReviewer::getPaperReviewerId, Function.identity()));
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
		paperReviewer.setAccount(ALIAS + formattedAccountNumber);
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

		// 1.找到審稿委員所擁有的公文檔案
		List<PaperReviewerFile> paperReviewerFilesByPaperReviewerId = paperReviewerFileService
				.getReviewerFilesByReviewerId(paperReviewerId);

		// 2.遍歷刪除公文檔案
		for (PaperReviewerFile paperReviewerFile : paperReviewerFilesByPaperReviewerId) {
			paperReviewerFileService.deleteReviewerFileById(paperReviewerFile.getPaperReviewerFileId());
		}

		// 3.最後刪除自身資料
		baseMapper.deleteById(paperReviewerId);
	}

	@Override
	public void deletePaperReviewerList(List<Long> paperReviewerIds) {
		for (Long paperReviewerId : paperReviewerIds) {
			this.deletePaperReviewer(paperReviewerId);
		}
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
		throw new AccountPasswordWrongException(messageHelper.get(I18nMessageKey.Registration.Auth.WRONG_ACCOUNT));
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

}
