package tw.com.topbs.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.convert.PaymentConvert;
import tw.com.topbs.mapper.MemberMapper;
import tw.com.topbs.mapper.OrdersMapper;
import tw.com.topbs.mapper.PaymentMapper;
import tw.com.topbs.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddAttendeesDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.pojo.entity.Payment;
import tw.com.topbs.service.AttendeesService;
import tw.com.topbs.service.OrdersService;
import tw.com.topbs.service.PaymentService;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {

	private final PaymentConvert paymentConvert;
	private final OrdersService ordersService;
	private final MemberMapper memberMapper;
	private final OrdersMapper ordersMapper;
	private final AttendeesService attendeesService;

	@Override
	public Payment getPayment(Long paymentId) {
		Payment payment = baseMapper.selectById(paymentId);
		return payment;
	}

	@Override
	public List<Payment> getPaymentList() {
		List<Payment> paymentList = baseMapper.selectList(null);
		return paymentList;
	}

	@Override
	public IPage<Payment> getPaymentPage(Page<Payment> page) {
		Page<Payment> paymentPage = baseMapper.selectPage(page, null);
		return paymentPage;
	}

	@Override
	@Transactional
	public void addPayment(ECPayResponseDTO ECPayResponseDTO) {
		// 轉換綠界金流offical Data 轉換 自己這邊的Entity
		Payment payment = paymentConvert.officalDataToEntity(ECPayResponseDTO);
		log.info("綠界金流回傳付款確認後資料：　" + payment);

		// 新增響應回來的交易紀錄
		baseMapper.insert(payment);

		// 當前回傳的訂單
		Orders currentOrders;

		// 如果付款成功，更新訂單的付款狀態
		if (payment.getRtnCode().equals("1")) {
			currentOrders = ordersService.getOrders(payment.getOrdersId());
			// 如果已經成功過就不用在更新成失敗
			if (currentOrders.getStatus() != 2) {
				// 2 代表付款成功，並更新這筆訂單資料
				currentOrders.setStatus(2);
				ordersService.updateById(currentOrders);

				log.info(currentOrders.getOrdersId() + " 付款成功，更新資料狀態");

				// 並將這個人更新進attendees表中，代表他已具備入場資格
				Member member = memberMapper.selectById(currentOrders.getMemberId());

				//付款完成，所以將他新增進 與會者名單
				AddAttendeesDTO addAttendeesDTO = new AddAttendeesDTO();
				addAttendeesDTO.setEmail(member.getEmail());
				addAttendeesDTO.setMemberId(member.getMemberId());
				attendeesService.addAfterPayment(addAttendeesDTO);

			}

		} else {
			currentOrders = ordersService.getOrders(payment.getOrdersId());
			// 如果已經成功過就不用在更新成失敗
			if (currentOrders.getStatus() != 2) {
				// 3 代表付款失敗，並更新這筆訂單資料
				currentOrders.setStatus(3);
				ordersService.updateById(currentOrders);

				log.warn(currentOrders.getOrdersId() + " 付款失敗，更新資料狀態");
			}
		}

		// 3.查詢這個訂單的會員
		Member member = memberMapper.selectById(currentOrders.getMemberId());

		// 4.判斷這個member有沒有group，是否處於團體報名，且付款的更新者為master，從如果有才進行此方法塊
		if (member.getGroupCode() != null && member.getGroupRole().equals("master")) {

			// 拿到所屬同一個團體報名的會員名單，並且是要group_role 為 slave的成員
			LambdaQueryWrapper<Member> memberQueryWrapper = new LambdaQueryWrapper<>();
			memberQueryWrapper.eq(Member::getGroupCode, member.getGroupCode()).eq(Member::getGroupRole, "slave");
			List<Member> groupMemberList = memberMapper.selectList(memberQueryWrapper);

			for (Member slaveMember : groupMemberList) {
				// 找到memberId為名單內成員且訂單的itemsSummary 為 註冊費的訂單，
				LambdaQueryWrapper<Orders> ordersQueryWrapper = new LambdaQueryWrapper<>();
				ordersQueryWrapper.eq(Orders::getMemberId, slaveMember.getMemberId())
						.eq(Orders::getItemsSummary, "Group Registration Fee");

				// 去更新其他slave(子報名者的付款狀態)
				Orders slaveMemberGroupOrder = ordersMapper.selectOne(ordersQueryWrapper);

				// 如果子報名者的付款狀態不是 付款成功，那個不管當前付款狀態如何，都更改
				if (slaveMemberGroupOrder.getStatus() != 2) {
					slaveMemberGroupOrder.setStatus(currentOrders.getStatus());
				}

				//如果已經為 2 付款成功，就不要去動它了
				ordersService.updateById(slaveMemberGroupOrder);

				
				// ✅ 只有付款成功才新增進 attendees
			    if (slaveMemberGroupOrder.getStatus() == 2) {
			        AddAttendeesDTO addAttendeesDTO = new AddAttendeesDTO();
			        addAttendeesDTO.setEmail(slaveMember.getEmail());
			        addAttendeesDTO.setMemberId(slaveMember.getMemberId());
			        attendeesService.addAfterPayment(addAttendeesDTO);
			    }

			}

		}

	}

	@Override
	public void updatePayment(PutPaymentDTO putPaymentDTO) {
		Payment payment = paymentConvert.putDTOToEntity(putPaymentDTO);
		baseMapper.updateById(payment);

	}

	@Override
	public void deletePayment(Long paymentId) {
		baseMapper.deleteById(paymentId);

	}

	@Override
	public void deletePaymentList(List<Long> paymentIds) {
		baseMapper.deleteBatchIds(paymentIds);
	}

}
