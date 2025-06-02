package tw.com.topbs.manager;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.mapper.PaperMapper;
import tw.com.topbs.pojo.entity.Paper;

@Component
@RequiredArgsConstructor
public class PaperManager {

	private final PaperMapper paperMapper;
	
	/**
	 * 獲得當前稿件總數
	 * 
	 * @return
	 */
	public Long getPaperCount() {
		return paperMapper.selectCount(null);
	}
	
	/**
	 * 根據memberId, 獲取會員的投稿列表
	 * 
	 * @param memberId
	 * @return
	 */
	public List<Paper> getPaperListByMemberId(Long memberId){
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(Paper::getMemberId, memberId);

		return paperMapper.selectList(paperQueryWrapper);
	};
	
	public IPage<Paper> getPaperPageByQuery(Page<Paper> pageable,String queryText,Integer status,String absType,String absProp){
		// 多條件篩選的組裝
		LambdaQueryWrapper<Paper> paperQueryWrapper = new LambdaQueryWrapper<>();
		paperQueryWrapper.eq(StringUtils.isNotBlank(absType), Paper::getAbsType, absType)
				.eq(StringUtils.isNotBlank(absProp), Paper::getAbsProp, absProp)
				.eq(status != null, Paper::getStatus, status)
				// 當 queryText 不為空字串、空格字串、Null 時才加入篩選條件
				.and(StringUtils.isNotBlank(queryText),
						wrapper -> wrapper.like(Paper::getAllAuthor, queryText)
								.or()
								.like(Paper::getAbsTitle, queryText)
								.or()
								.like(Paper::getPublicationGroup, queryText)
								.or()
								.like(Paper::getPublicationNumber, queryText)
								.or()
								.like(Paper::getCorrespondingAuthorPhone, queryText)
								.or()
								.like(Paper::getCorrespondingAuthorEmail, queryText));
		
		
		return paperMapper.selectPage(pageable, paperQueryWrapper);
		
	}
	
	
	
	
}
