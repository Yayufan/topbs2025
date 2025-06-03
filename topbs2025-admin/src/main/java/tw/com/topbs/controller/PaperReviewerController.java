package tw.com.topbs.controller;

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
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.SaTokenInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.PaperReviewerLoginInfo;
import tw.com.topbs.pojo.DTO.SendEmailByTagDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagToPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.VO.PaperReviewerVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.PaperReviewer;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.PaperReviewerService;
import tw.com.topbs.utils.R;

@Tag(name = "審稿委員API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paperReviewer")
public class PaperReviewerController {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;
	
	private final PaperReviewerService paperReviewerService;

	@GetMapping("{id}")
	@Operation(summary = "查詢單一審稿委員")
	public R<PaperReviewerVO> getPaperReviewer(@PathVariable("id") Long paperReviewerId) {
		PaperReviewerVO paperReviewerVO = paperReviewerService.getPaperReviewer(paperReviewerId);
		return R.ok(paperReviewerVO);
	}

	@GetMapping
	@Operation(summary = "查詢全部審稿委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<List<PaperReviewerVO>> getPaperReviewerList() {
		List<PaperReviewerVO> paperReviewerVOList = paperReviewerService.getPaperReviewerList();
		return R.ok(paperReviewerVOList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部審稿委員(分頁)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<PaperReviewerVO>> getPaperReviewerPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<PaperReviewer> pageable = new Page<PaperReviewer>(page, size);
		IPage<PaperReviewerVO> paperReviewerVOPage = paperReviewerService.getPaperReviewerPage(pageable);
		return R.ok(paperReviewerVOPage);
	}

	@PostMapping
	@Operation(summary = "新增單一審稿委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<PaperReviewer> savePaperReviewer(@RequestBody @Valid AddPaperReviewerDTO addPaperReviewerDTO) {
		paperReviewerService.addPaperReviewer(addPaperReviewerDTO);
		return R.ok();
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "修改審稿委員")
	@SaCheckRole("super-admin")
	public R<PaperReviewer> updatePaperReviewer(@RequestBody @Valid PutPaperReviewerDTO putPaperReviewerDTO) {
		paperReviewerService.updatePaperReviewer(putPaperReviewerDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "刪除審稿委員")
	public R<PaperReviewer> deletePaperReviewer(@PathVariable("id") Long paperReviewerId) {
		paperReviewerService.deletePaperReviewer(paperReviewerId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除審稿委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeletePaperReviewer(@RequestBody List<Long> ids) {
		paperReviewerService.deletePaperReviewerList(ids);
		return R.ok();

	}

	@Operation(summary = "為 審稿委員 新增/更新/刪除 複數標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("tag")
	public R<Void> assignTagToPaperReviewer(@Validated @RequestBody AddTagToPaperReviewerDTO addTagToPaperReviewerDTO) {
		paperReviewerService.assignTagToPaperReviewer(addTagToPaperReviewerDTO.getTargetTagIdList(),
				addTagToPaperReviewerDTO.getPaperReviewerId());
		return R.ok();
	}

	/** 以下與寄送給 審稿委員 信件有關 */
	@Operation(summary = "寄送信件給審稿委員，可根據tag來篩選寄送")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PostMapping("send-email")
	public R<Void> sendEmailToPaperReviewers(@Validated @RequestBody SendEmailByTagDTO sendEmailByTagDTO) {
		paperReviewerService.sendEmailToPaperReviewers(sendEmailByTagDTO.getTagIdList(),
				sendEmailByTagDTO.getSendEmailDTO());
		return R.ok();

	}
	
	
	/** 以下是審稿委員自己使用的API */
	/** 以下與審稿委員登入有關 */
	
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
	
	@Operation(summary = "審稿委員登入")
	@PostMapping("login")
	public R<SaTokenInfo> login(@Validate @RequestBody PaperReviewerLoginInfo paperReviewerLoginInfo) {

		// 透過key 獲取redis中的驗證碼
		String redisCode = redissonClient.<String>getBucket(paperReviewerLoginInfo.getVerificationKey()).get();
		String userVerificationCode = paperReviewerLoginInfo.getVerificationCode();

		// 判斷驗證碼是否正確,如果不正確就直接返回前端,不做後續的業務處理
		if (userVerificationCode == null || redisCode == null
				|| !redisCode.equals(userVerificationCode.trim().toLowerCase())) {
			return R.fail("Verification code is incorrect");
		}

		// 驗證通過,刪除key 並往後執行添加操作
		redissonClient.getBucket(paperReviewerLoginInfo.getVerificationKey()).delete();
		SaTokenInfo tokenInfo = paperReviewerService.login(paperReviewerLoginInfo);
		return R.ok(tokenInfo);
	}

	@Operation(summary = "審稿委員登出")
	@Parameters({
			@Parameter(name = "Authorization-paper-reviewer", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.PAPER_REVIEWER_TYPE)
	@PostMapping("logout")
	public R<Void> logout() {
		paperReviewerService.logout();
		return R.ok();
	}
	
	@Operation(summary = "獲取緩存內的審稿委員資訊")
	@SaCheckLogin(type = StpKit.PAPER_REVIEWER_TYPE)
	@Parameters({
			@Parameter(name = "Authorization-paper-reviewer", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER), })
	@GetMapping("getPaperReviewerInfo")
	public R<PaperReviewer> GetUserInfo() {

		// 獲取token 對應審稿資料
		PaperReviewer paperReviewerInfo = paperReviewerService.getPaperReviewerInfo();

		// 返回會員資料
		return R.ok(paperReviewerInfo);

	}
	

}
