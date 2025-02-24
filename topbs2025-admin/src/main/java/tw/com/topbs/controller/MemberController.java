package tw.com.topbs.controller;

import java.util.List;

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

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaTokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.MemberConvert;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.utils.R;

@Tag(name = "會員API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
public class MemberController {

	private final MemberService memberService;
	private final MemberConvert memberConvert;

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

	@PostMapping
	@Operation(summary = "新增單一會員，也就是註冊功能")
	public R<SaTokenInfo> saveMember(@RequestBody @Valid AddMemberDTO addMemberDTO) {
		SaTokenInfo tokenInfo = memberService.addMember(addMemberDTO);
		return R.ok(tokenInfo);
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

}
