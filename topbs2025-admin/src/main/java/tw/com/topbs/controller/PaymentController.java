package tw.com.topbs.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PaymentConvert;
import tw.com.topbs.manager.OrderPaymentManager;
import tw.com.topbs.pojo.DTO.ECPayDTO.ECPayResponseDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaymentDTO;
import tw.com.topbs.pojo.VO.PaymentVO;
import tw.com.topbs.pojo.entity.Payment;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.PaymentService;
import tw.com.topbs.utils.R;

@Tag(name = "交易明細紀錄API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/payment")
public class PaymentController {
	private final PaymentService paymentService;
	private final PaymentConvert paymentConvert;
	private final OrderPaymentManager orderPaymentManager;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一交易明細紀錄")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Payment> getPayment(@PathVariable("id") Long paymentId) {
		Payment payment = paymentService.getPayment(paymentId);
		return R.ok(payment);
	}

	@GetMapping
	@Operation(summary = "查詢全部交易明細紀錄")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<PaymentVO>> getUserList() {
		List<Payment> paymentList = paymentService.getPaymentList();
		List<PaymentVO> paymentVOList = paymentConvert.entityListToVOList(paymentList);
		return R.ok(paymentVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部交易明細紀錄(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<Payment>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Payment> pageable = new Page<Payment>(page, size);
		IPage<Payment> paymentPage = paymentService.getPaymentPage(pageable);
		return R.ok(paymentPage);
	}

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@Operation(summary = "接收綠界回傳資料，新增單一交易明細紀錄")
	public String savePayment(@ModelAttribute @Valid ECPayResponseDTO ECPayResponseDTO) {
		System.out.println(ECPayResponseDTO);
		orderPaymentManager.handleEcpayCallback(ECPayResponseDTO);
		return "1|OK";
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "修改交易明細紀錄")
	public R<Payment> updatePayment(@RequestBody @Valid PutPaymentDTO putPaymentDTO) {
		paymentService.updatePayment(putPaymentDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除交易明細紀錄")
	public R<Payment> deletePayment(@PathVariable("id") Long paymentId) {
		paymentService.deletePayment(paymentId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除交易明細紀錄")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeletePayment(@RequestBody List<Long> ids) {
		paymentService.deletePaymentList(ids);
		return R.ok();

	}
}
