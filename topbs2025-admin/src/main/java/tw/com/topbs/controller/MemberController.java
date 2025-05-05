package tw.com.topbs.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RedissonClient;
import org.simpleframework.xml.core.Validate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wf.captcha.SpecCaptcha;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaTokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.MemberConvert;
import tw.com.topbs.exception.RegistrationInfoException;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.ForgetPwdDTO;
import tw.com.topbs.pojo.DTO.GroupRegistrationDTO;
import tw.com.topbs.pojo.DTO.MemberLoginInfo;
import tw.com.topbs.pojo.DTO.PutMemberIdDTO;
import tw.com.topbs.pojo.DTO.SendEmailByTagDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagToMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Orders;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.utils.R;

@Tag(name = "會員API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
public class MemberController {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private final MemberService memberService;
	private final MemberConvert memberConvert;

	@GetMapping("/captcha")
	@Operation(summary = "獲取驗證碼")
	public R<HashMap<Object, Object>> captcha() {
		SpecCaptcha specCaptcha = new SpecCaptcha(130, 50, 5);
		String verCode = specCaptcha.text().toLowerCase();
		String key = "Captcha:" + UUID.randomUUID().toString();
		// 明確調用String類型的Bucket,存入String類型的Value 進redis並設置過期時間為30分鐘
		redissonClient.<String>getBucket(key).set(verCode, 30, TimeUnit.MINUTES);

		// 将key和base64返回给前端
		HashMap<Object, Object> hashMap = new HashMap<>();
		hashMap.put("key", key);
		hashMap.put("image", specCaptcha.toBase64());

		return R.ok(hashMap);
	}

	@GetMapping("owner")
	@Operation(summary = "查詢單一會員For會員本人")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Member> getMemberForOwner() {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();
		Member member = memberService.getMember(memberCache.getMemberId());
		return R.ok(member);
	}

	@GetMapping("{id}")
	@Operation(summary = "查詢單一會員For管理者")
	@SaCheckRole("super-admin")
	public R<Member> getMember(@PathVariable("id") Long memberId) {
		Member member = memberService.getMember(memberId);
		return R.ok(member);
	}

	@GetMapping
	@Operation(summary = "查詢全部會員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<MemberVO>> getUserList() {
		List<Member> memberList = memberService.getMemberList();
		List<MemberVO> memberVOList = memberConvert.entityListToVOList(memberList);
		return R.ok(memberVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部會員(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<Member>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Member> pageable = new Page<Member>(page, size);
		IPage<Member> memberPage = memberService.getMemberPage(pageable);
		return R.ok(memberPage);
	}

	@GetMapping("count")
	@Operation(summary = "查詢全部會員總數")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Long> getMemberCount() {
		Long memberCount = memberService.getMemberCount();
		return R.ok(memberCount);
	}

	@GetMapping("count-by-order-status")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "根據訂單繳費狀態,查詢相符的會員總數")
	public R<Integer> getMemberCountByStatus(String status) {

		Integer memberCount = memberService.getMemberOrderCount(status);
		return R.ok(memberCount);
	}

	@GetMapping("member-and-order")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "根據訂單繳費狀態,查詢相符的會員列表")
	public R<IPage<MemberOrderVO>> getMemberOrder(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "queryText", required = false) String queryText) {
		Page<Orders> pageable = new Page<Orders>(page, size);
		IPage<MemberOrderVO> memberOrderVO = memberService.getMemberOrderVO(pageable, status, queryText);

		return R.ok(memberOrderVO);
	}

	@GetMapping("unpaid-member")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "根據條件,查詢註冊費未付款的台灣會員列表")
	public R<IPage<MemberVO>> getUnpaidMember(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(value = "queryText", required = false) String queryText) {
		Page<Member> pageable = new Page<Member>(page, size);
		IPage<MemberVO> unpaidMemberList = memberService.getUnpaidMemberList(pageable, queryText);

		return R.ok(unpaidMemberList);
	}

	@PostMapping
	@Operation(summary = "新增單一會員，也就是註冊功能")
	public R<SaTokenInfo> saveMember(@RequestBody @Valid AddMemberDTO addMemberDTO) throws RegistrationInfoException {
		// 透過key 獲取redis中的驗證碼
		String redisCode = redissonClient.<String>getBucket(addMemberDTO.getVerificationKey()).get();
		String userVerificationCode = addMemberDTO.getVerificationCode();

		// 判斷驗證碼是否正確,如果不正確就直接返回前端,不做後續的業務處理
		if (userVerificationCode == null || redisCode == null
				|| !redisCode.equals(userVerificationCode.trim().toLowerCase())) {
			return R.fail("Verification code is incorrect");
		}

		// 驗證通過,刪除key 並往後執行添加操作
		redissonClient.getBucket(addMemberDTO.getVerificationKey()).delete();
		SaTokenInfo tokenInfo = memberService.addMember(addMemberDTO);

		return R.ok(tokenInfo);
	}

	@PostMapping("admin")
	@Operation(summary = "新增單一會員For管理者")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	public R<Void> saveMemberForAdmin(@RequestBody @Valid AddMemberForAdminDTO addMemberForAdminDTO) {
		memberService.addMemberForAdmin(addMemberForAdminDTO);
		return R.ok();
	}

	@PostMapping("group")
	@Operation(summary = "團體報名，也就是團體註冊功能")
	public R<Void> groupRegistration(@RequestBody @Valid GroupRegistrationDTO groupRegistrationDTO)
			throws RegistrationInfoException {
		// 透過key 獲取redis中的驗證碼
		String redisCode = redissonClient.<String>getBucket(groupRegistrationDTO.getVerificationKey()).get();
		String userVerificationCode = groupRegistrationDTO.getVerificationCode();

		// 判斷驗證碼是否正確,如果不正確就直接返回前端,不做後續的業務處理
		if (userVerificationCode == null || redisCode == null
				|| !redisCode.equals(userVerificationCode.trim().toLowerCase())) {
			return R.fail("Verification code is incorrect");
		}

		// 驗證通過,刪除key 並往後執行添加操作
		redissonClient.getBucket(groupRegistrationDTO.getVerificationKey()).delete();
		memberService.addGroupMember(groupRegistrationDTO);

		return R.ok();
	}

	@PutMapping("owner")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Operation(summary = "修改會員資料For會員本人")
	public R<Member> updateMemberForOwner(@RequestBody @Valid PutMemberDTO putMemberDTO) {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();
		if (memberCache.getMemberId().equals(putMemberDTO.getMemberId())) {
			memberService.updateMember(putMemberDTO);
			return R.ok();
		}

		return R.fail("The Token is not the user's own and cannot retrieve non-user's information.");

	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "修改會員資料For管理者")
	@SaCheckRole("super-admin")
	public R<Member> updateMember(@RequestBody @Valid PutMemberDTO putMemberDTO) {
		// 直接更新會員
		memberService.updateMember(putMemberDTO);
		return R.ok();

	}

	@PutMapping("unpaid-member")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "更新註冊費未付款的台灣會員，狀態改為已付款")
	public R<Void> getUnpaidMember(@RequestBody @Valid PutMemberIdDTO putMemberIdDTO) {
		memberService.approveUnpaidMember(putMemberIdDTO.getMemberId());
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除會員")
	public R<Member> deleteMember(@PathVariable("id") Long memberId) {
		memberService.deleteMember(memberId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除會員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeleteMember(@RequestBody List<Long> ids) {
		memberService.deleteMemberList(ids);
		return R.ok();

	}

	@Operation(summary = "獲取緩存內的會員資訊")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER), })
	@GetMapping("getMemberInfo")
	public R<Member> GetUserInfo() {

		// 獲取token 對應會員資料
		Member memberInfo = memberService.getMemberInfo();

		// 返回會員資料
		return R.ok(memberInfo);

	}

	/** 以下與會員登入有關 */
	@Operation(summary = "會員登入")
	@PostMapping("login")
	public R<SaTokenInfo> login(@Validate @RequestBody MemberLoginInfo memberLoginInfo) {

		// 透過key 獲取redis中的驗證碼
		String redisCode = redissonClient.<String>getBucket(memberLoginInfo.getVerificationKey()).get();
		String userVerificationCode = memberLoginInfo.getVerificationCode();

		// 判斷驗證碼是否正確,如果不正確就直接返回前端,不做後續的業務處理
		if (userVerificationCode == null || redisCode == null
				|| !redisCode.equals(userVerificationCode.trim().toLowerCase())) {
			return R.fail("Verification code is incorrect");
		}

		// 驗證通過,刪除key 並往後執行添加操作
		redissonClient.getBucket(memberLoginInfo.getVerificationKey()).delete();
		SaTokenInfo tokenInfo = memberService.login(memberLoginInfo);
		return R.ok(tokenInfo);
	}

	@Operation(summary = "會員登出")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@PostMapping("logout")
	public R<Void> logout() {
		memberService.logout();
		return R.ok();
	}

	@Operation(summary = "找回密碼")
	@PostMapping("forget-password")
	public R<Void> forgetPassword(@Validated @RequestBody ForgetPwdDTO forgetPwdDTO) throws MessagingException {
		memberService.forgetPassword(forgetPwdDTO.getEmail());
		return R.ok("A password retrieval email has been sent to your mailbox");
	}

	/** 以下是跟Tag有關的Controller */

	@Operation(summary = "根據會員ID 查詢會員資料及他持有的標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@GetMapping("tag/{id}")
	public R<MemberTagVO> getMemberTagVOByMember(@PathVariable("id") Long memberId) {
		MemberTagVO memberTagVOByMember = memberService.getMemberTagVOByMember(memberId);
		return R.ok(memberTagVOByMember);

	}

	@Operation(summary = "查詢所有會員資料及他持有的標籤(分頁)")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("tag/pagination")
	public R<IPage<MemberTagVO>> getAllMemberTagVO(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Member> pageInfo = new Page<>(page, size);

		IPage<MemberTagVO> memberTagVOPage = memberService.getAllMemberTagVO(pageInfo);
		return R.ok(memberTagVOPage);
	}

	@Operation(summary = "根據條件 查詢會員資料及他持有的標籤(分頁)")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("tag/pagination-by-query")
	public R<IPage<MemberTagVO>> getAllMemberTagVOByQuery(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String queryText, @RequestParam(required = false) Integer status) {

		Page<Member> pageInfo = new Page<>(page, size);
		IPage<MemberTagVO> memberList;

		memberList = memberService.getAllMemberTagVOByQuery(pageInfo, queryText, status);

		return R.ok(memberList);
	}

	@Operation(summary = "為會員新增/更新/刪除 複數標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("tag")
	public R<Void> assignTagToMember(@Validated @RequestBody AddTagToMemberDTO addTagToMemberDTO) {
		memberService.assignTagToMember(addTagToMemberDTO.getTargetTagIdList(), addTagToMemberDTO.getMemberId());
		return R.ok();

	}

	/** 以下與寄送給會員信件有關 */
	@Operation(summary = "寄送信件給會員，可根據tag來篩選寄送")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PostMapping("send-email")
	public R<Void> sendEmailToMembers(@Validated @RequestBody SendEmailByTagDTO sendEmailByTagDTO) {
		memberService.sendEmailToMembers(sendEmailByTagDTO.getTagIdList(), sendEmailByTagDTO.getSendEmailDTO());
		return R.ok();

	}

	@Operation(summary = "下載會員excel列表")
	@SaCheckRole("super-admin")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("/download-excel")
	public void downloadExcel(HttpServletResponse response) throws IOException {
		memberService.downloadExcel(response);
	}

}
