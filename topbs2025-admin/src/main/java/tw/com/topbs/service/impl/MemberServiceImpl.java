package tw.com.topbs.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.MemberConvert;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddOrdersItemDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.OrdersItemService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.system.pojo.VO.SysUserVO;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

	private final MemberConvert memberConvert;
	private final OrdersService ordersService;
	private final OrdersItemService ordersItemService;

	@Override
	public Member getMember(Long memberId) {
		Member member = baseMapper.selectById(memberId);
		return member;
	}

	@Override
	public List<Member> getMemberList() {
		List<Member> memberList = baseMapper.selectList(null);
		return memberList;
	}

	@Override
	public IPage<Member> getMemberPage(Page<Member> page) {
		Page<Member> memberPage = baseMapper.selectPage(page, null);
		return memberPage;
	}

	@Override
	@Transactional
	public SaTokenInfo addMember(AddMemberDTO addMemberDTO) {

		//獲取設定上的早鳥優惠、一般金額、及最後註冊時間
		
		
		// 首先新增這個會員資料
		Member member = memberConvert.addDTOToEntity(addMemberDTO);
		baseMapper.insert(member);

		// 然後開始新建 繳費訂單
		AddOrdersDTO addOrdersDTO = new AddOrdersDTO();
		// 設定 會員ID
		addOrdersDTO.setMemberId(member.getMemberId());
		// 設定繳費狀態為 未繳費
		addOrdersDTO.setStatus(0);

		
		
		
		// 設定會費 整數1000塊台幣，應該會根據早鳥優惠進行金額變動
		BigDecimal amount = BigDecimal.valueOf(1000L);
		addOrdersDTO.setTotalAmount(amount);

		// 透過訂單服務 新增訂單
		Long ordersId = ordersService.addOrders(addOrdersDTO);

		// 因為是綁在註冊時的訂單產生，所以這邊要再設定訂單的細節
		AddOrdersItemDTO addOrdersItemDTO = new AddOrdersItemDTO();
		// 設定 基本資料
		addOrdersItemDTO.setOrdersId(ordersId);
		addOrdersItemDTO.setProductType("Registration Fee");
		addOrdersItemDTO.setProductName("2025 TOPBS Registration Fee ");

		// 設定 單價、數量、小計
		addOrdersItemDTO.setUnitPrice(amount);
		addOrdersItemDTO.setQuantity(1);
		addOrdersItemDTO.setSubtotal(amount);

		// 透過訂單明細服務 新增訂單
		ordersItemService.addOrdersItem(addOrdersItemDTO);

		// 之後應該要以這個會員ID 產生Token 回傳前端，讓他直接進入登入狀態
		StpKit.MEMBER.login(member.getMemberId());

		// 登入後才能取得session
		SaSession session = StpKit.MEMBER.getSession();
		// 並對此token 設置會員的緩存資料
		session.set("memberInfo", member);

		SaTokenInfo tokenInfo = StpKit.MEMBER.getTokenInfo();
		return tokenInfo;

	}

	@Override
	public void updateMember(PutMemberDTO putMemberDTO) {
		Member member = memberConvert.putDTOToEntity(putMemberDTO);
		baseMapper.updateById(member);
	}

	@Override
	public void deleteMember(Long memberId) {
		baseMapper.deleteById(memberId);
	}

	@Override
	public void deleteMemberList(List<Long> memberIds) {
		baseMapper.deleteBatchIds(memberIds);
	}

	@Override
	public Member getMemberInfo() {
		// 會員登入後才能取得session
		SaSession session = StpKit.MEMBER.getSession();
		// 獲取當前使用者的資料
		Member memberInfo = (Member) session.get("memberInfo");
		return memberInfo;
	}

}
