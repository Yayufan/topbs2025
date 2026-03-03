package tw.com.topbs.manager;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.MemberConvert;
import tw.com.topbs.helper.TagAssignmentHelper;
import tw.com.topbs.pojo.BO.MemberExcelRaw;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.excelPojo.MemberExcel;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.AttendeesTagService;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.TagService;

/**
 * 管理會員 和 訂單的需求,<br>
 * 以及成為與會者流程組裝
 */
@Component
@RequiredArgsConstructor
public class MemberOrderManager {
	
	private final TagAssignmentHelper tagAssignmentHelper;
	private final MemberConvert memberConvert;
	private final MemberService memberService;
	private final OrdersService ordersService;
	private final AttendeesService attendeesService;
	private final AttendeesTagService attendeesTagService;
	private final TagService tagService;

	// --------------------------- 查詢相關 ---------------------------------------

	/**
	 * 獲得訂單狀態的會員人數
	 * 
	 * @param status
	 * @return
	 */
	public Integer getMemberOrderCount(Integer status) {

		// 1.查找符合訂單狀態的訂單
		List<Orders> registrationOrderList = ordersService.getRegistrationOrderListByStatus(status);

		// 2.返回當前訂單狀態的會員總人數
		return memberService.getMemberOrderCount(registrationOrderList);

	}

	/**
	 * 獲得會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param status
	 * @param queryText
	 * @return
	 */
	public IPage<MemberOrderVO> getMemberOrderVO(Page<Orders> page, Integer status, String queryText) {
		// 1.根據分頁 和 訂單狀態, 拿到分頁對象
		Page<Orders> orderPage = ordersService.getRegistrationOrderPageByStatus(page, status);

		// 2.再把訂單分頁 和 會員的查詢條件放入,拿到VO對象並返回
		IPage<MemberOrderVO> memberOrderVO = memberService.getMemberOrderVO(orderPage, status, queryText);
		return memberOrderVO;
	}

	/**
	 * 適用於不使用金流,人工審核<br>
	 * 獲得本國未付款的 會員及其訂單的VO對象
	 * 
	 * @param page
	 * @param queryText
	 * @return
	 */
	public IPage<MemberVO> getUnpaidMemberPage(Page<Member> page, String queryText) {

		// 1.本國籍的快速搜索 (外國團體報名不在此限)
		List<Orders> unpaidRegistrationOrderList = ordersService.getUnpaidRegistrationOrderList();

		// 2.拿到本國籍,未付款的分頁對象
		IPage<MemberVO> unpaidMemberPage = memberService.getUnpaidMemberPage(page, unpaidRegistrationOrderList,
				queryText);
		return unpaidMemberPage;
	}

	/**
	 * 管理者手動更改付款狀態<br>
	 * 適用於非系統金流收款的狀態<br>
	 * 變更成付款狀態時,新增進與會者名單,並配置Tag
	 * 
	 * @param memberId
	 */
	public void approveUnpaidMember(Long memberId) {
		// 1.新會員的註冊費訂單狀態 => 已付款
		ordersService.approveUnpaidMember(memberId);

		// 2.拿到Member資訊
		Member member = memberService.getMember(memberId);

		// 3.由後台新增的Member , 自動付款完成，新增進與會者名單
		Attendees attendees = attendeesService.addAttendees(member);

		// 4.獲取當下與會者群體的Index,進行與會者標籤分組
		tagAssignmentHelper.assignTag(attendees.getAttendeesId(),
				attendeesService::getAttendeesGroupIndex,
				tagService::getOrCreateAttendeesGroupTag,
				attendeesTagService::addAttendeesTag);
		
	}
	
	/**
	 * 下載所有會員列表, 其中包含他們當前的付款狀態
	 * 
	 * @param response
	 * @throws IOException
	 */
	public void downloadExcel(HttpServletResponse response) throws IOException {
		// 1.設置Excel 檔案資訊
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("會員名單", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");
		
		// 2.獲取 會員ID-註冊費訂單 的映射對象
		Map<Long, Orders> ordersMap = ordersService.getRegistrationOrderMapByMemberId();
		
		// 3.高效率獲取所有會員資料
		List<Member> memberList = memberService.getMembersEfficiently();
		
		// 4.遍歷會員資料,組裝excelVO對象
		List<MemberExcel> excelData = memberList.stream().map(member -> {
			// 4-1 獲取該會員的訂單
			Orders orders = ordersMap.get(member.getMemberId());

			// 4-2 轉換設置資料
			MemberExcelRaw memberExcelRaw = memberConvert.entityToExcelRaw(member);
			memberExcelRaw.setStatus(orders.getStatus());
			memberExcelRaw.setRegistrationFee(orders.getTotalAmount());
			MemberExcel memberExcel = memberConvert.memberExcelRawToExcel(memberExcelRaw);

			return memberExcel;

		}).toList();

		// 5.輸出成Excel
		EasyExcel.write(response.getOutputStream(), MemberExcel.class).sheet("會員列表").doWrite(excelData);

		
	}

}
