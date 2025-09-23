package tw.com.topbs.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Sets;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Tag;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.MemberTagService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.TagService;

@Component
@RequiredArgsConstructor
public class MemberTagManager {

	private final MemberService memberService;
	private final OrdersService ordersService;
	private final MemberTagService memberTagService;
	private final TagService tagService;

	/**
	 * 根據 memberId, 獲取MemberTagVO 對象
	 * 
	 * @param memberId
	 * @return
	 */
	public MemberTagVO getMemberTagVOByMember(Long memberId) {
		// 1.獲取基本的 memberTagVO 對象
		MemberTagVO memberTagVO = memberService.getMemberTagVOByMember(memberId);

		// 2.獲取註冊費訂單，放入VO中
		Orders registrationOrder = ordersService.getRegistrationOrderByMemberId(memberId);
		memberTagVO.setAmount(registrationOrder.getTotalAmount());

		// 3.查詢該member所有關聯的tagId Set
		Set<Long> tagIdSet = memberTagService.getTagIdsByMemberId(memberId);

		// 4.如果沒有任何關聯,就可以直接返回了
		if (tagIdSet.isEmpty()) {
			return memberTagVO;
		}

		// 5.去Tag表中查詢實際的Tag資料，並轉換成Set集合
		List<Tag> tagList = tagService.getTagByTagIds(tagIdSet);

		// 6.最後填入memberTagVO對象並返回
		memberTagVO.setTagList(tagList);
		return memberTagVO;
	};

	/**
	 * 根據搜尋條件 獲取會員資料及持有的tag集合(分頁)
	 * 
	 * @param page
	 * @param queryText
	 * @param status
	 * @return
	 */
	IPage<MemberTagVO> getMemberTagVOByQuery(Page<Member> page, String queryText, Integer status) {

		// 初始化返回對象
		IPage<MemberTagVO> voPage = new Page<>(page.getCurrent(), page.getSize());
		// 初始化,符合status條件的memberIds
		List<Long> memberIdsByStatus = new ArrayList<>();

		// 1.status 為註冊費訂單的付款狀態,所以得先抽出判斷
		if (status != null) {
			// 1-1 找到items_summary 符合 Registration Fee ，且status符合篩選條件的資料
			List<Orders> registrationOrderList = ordersService.getRegistrationOrderListByStatus(status);
			// 1-2 如果沒查到符合的訂單,直接返回VO對象,因為同時沒有符合的會員資料
			if (registrationOrderList.isEmpty()) {
				return voPage;
			}

			// 1-3 將符合的memberIds提取出
			memberIdsByStatus = registrationOrderList.stream()
					.map(order -> order.getMemberId())
					.collect(Collectors.toList());
		}
		
		// 2.
		
		

		return null;
	}

	/**
	 * 為用戶新增/更新/刪除 複數tag
	 * 
	 * @param targetTagIdList
	 * @param memberId
	 */
	@Transactional
	public void assignTagToMember(List<Long> targetTagIdList, Long memberId) {

		// 1.拿到目標 TagIdSet
		Set<Long> targetTagIdSet = new HashSet<>(targetTagIdList);

		// 2.查詢該member所有關聯的tagId Set
		Set<Long> currentTagIdSet = memberTagService.getTagIdsByMemberId(memberId);

		// 3.拿到該移除的集合 和 該新增的集合
		Set<Long> tagsToRemove = Sets.difference(currentTagIdSet, targetTagIdSet);
		Set<Long> tagsToAdd = Sets.difference(targetTagIdSet, currentTagIdSet);

		// 4. 執行刪除操作，如果 需刪除集合 中不為空，則開始刪除
		if (!tagsToRemove.isEmpty()) {
			memberTagService.removeTagsFromMember(memberId, tagsToRemove);
		}

		// 5.執行新增操作
		if (!tagsToAdd.isEmpty()) {
			memberTagService.addTagsToMember(memberId, tagsToAdd);
		}

	}

}
