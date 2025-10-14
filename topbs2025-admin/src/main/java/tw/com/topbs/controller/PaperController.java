package tw.com.topbs.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.enums.ReviewStageEnum;
import tw.com.topbs.exception.RedisKeyException;
import tw.com.topbs.manager.PaperManager;
import tw.com.topbs.manager.PaperReviewManager;
import tw.com.topbs.manager.PaperTagManager;
import tw.com.topbs.pojo.DTO.AddSlideUploadDTO;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.PutSlideUploadDTO;
import tw.com.topbs.pojo.DTO.ReviewStageDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerToPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagToPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperTagVO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.pojo.entity.PaperFileUpload;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.system.pojo.VO.CheckFileVO;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.service.SysChunkFileService;
import tw.com.topbs.utils.MinioUtil;
import tw.com.topbs.utils.R;

@Tag(name = "稿件API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paper")
public class PaperController {

	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

	private final PaperService paperService;
	private final MemberService memberService;
	private final SysChunkFileService sysChunkFileService;
	private final PaperManager paperManager;
	private final PaperTagManager paperTagManager;
	private final PaperReviewManager paperReviewerManager;
	private final MinioUtil minioUtil;

	/** ----------------- 投稿者使用的API ------------------------- */

	@GetMapping("owner/{id}")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "查詢會員自身的單一稿件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<PaperVO> getPaperForOwner(@PathVariable("id") Long paperId) {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();
		PaperVO vo = paperManager.getPaperVO(paperId, memberCache.getMemberId());
		return R.ok(vo);
	}

	@GetMapping("owner")
	@Operation(summary = "查詢會員自身的全部稿件")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<List<PaperVO>> getPaperListForOwner() {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();
		List<PaperVO> voList = paperManager.getPaperVOList(memberCache.getMemberId());
		return R.ok(voList);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "新增單一稿件", description = "請使用formData包裝,兩個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.files(value = array)<br>" + "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> savePaper(@RequestPart("files") @Schema(name = "files", type = "array") MultipartFile[] files,
			@RequestPart("data") @Schema(name = "data", implementation = AddPaperDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		// 處理Java 8 LocalDate 和 LocalDateTime的轉換
		objectMapper.registerModule(new JavaTimeModule());
		AddPaperDTO addPaperDTO = objectMapper.readValue(jsonData, AddPaperDTO.class);

		// 將檔案和資料對象傳給後端
		paperManager.addPaper(files, addPaperDTO);

		return R.ok();
	}

	@PutMapping(value = "owner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "會員修改自身單一稿件", description = "請使用formData包裝,兩個key <br>" + "1.data(value = DTO(json))<br>"
			+ "2.files(value = array)<br>" + "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> updatePaper(@RequestPart("files") @Schema(name = "files", type = "array") MultipartFile[] files,
			@RequestPart("data") @Schema(name = "data", implementation = PutPaperDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		// 處理Java 8 LocalDate 和 LocalDateTime的轉換
		objectMapper.registerModule(new JavaTimeModule());
		PutPaperDTO putPaperDTO = objectMapper.readValue(jsonData, PutPaperDTO.class);

		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		// 判斷更新資料中的memberId 是否與memberCache的memberId一致
		if (putPaperDTO.getMemberId().equals(memberCache.getMemberId())) {
			paperManager.updatePaper(files, putPaperDTO);
			return R.ok();
		} else {
			return R.fail(
					"Please do not maliciously tamper with other people's information. Legal measures will be taken after verification.");
		}

	}

	@DeleteMapping("owner/{id}")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "會員刪除自身的單一稿件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> deletePaperForOwner(@PathVariable("id") Long paperId) {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		paperManager.deletePaper(paperId, memberCache.getMemberId());
		return R.ok();
	}

	/** ----------------- 管理者使用的API ------------------------- */
	/** 第一階段 API */
	@GetMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "查詢單一稿件For後台")
	@SaCheckRole("super-admin")
	public R<PaperTagVO> getPaperTagVO(@PathVariable("id") Long paperId) {
		PaperTagVO vo = paperTagManager.getPaperTagVO(paperId);
		return R.ok(vo);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部稿件(分頁)For後台管理")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<PaperTagVO>> getPaperTagVOPage(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String queryText, @RequestParam(required = false) Integer status,
			@RequestParam(required = false) String absType, @RequestParam(required = false) String absProp) {
		Page<Paper> pageable = new Page<Paper>(page, size);
		IPage<PaperTagVO> voPage = paperTagManager.getPaperTagVOPage(pageable, queryText, status, absType, absProp);
		return R.ok(voPage);
	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "修改單一稿件 For管理者")
	public R<Void> updatePaperForAdmin(@RequestBody @Valid PutPaperForAdminDTO putPaperForAdminDTO) {
		paperManager.updatePaperForAdmin(putPaperForAdminDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除單一稿件 For管理者")
	@SaCheckRole("super-admin")
	public R<Void> deletePaper(@PathVariable("id") Long paperId) {
		paperManager.deletePaper(paperId);
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除稿件 For管理者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeletePaper(@RequestBody List<Long> paperIds) {
		paperManager.deletePaperList(paperIds);
		return R.ok();

	}

	@Operation(summary = "為稿件新增/更新/刪除 複數標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("tag")
	public R<Void> assignTagToPaper(@Validated @RequestBody AddTagToPaperDTO addTagToPaperDTO) {
		paperTagManager.assignTagToPaper(addTagToPaperDTO.getTargetTagIdList(), addTagToPaperDTO.getPaperId());
		return R.ok();
	}

	@Operation(summary = "下載稿件 評分結果excel列表，For管理者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@GetMapping("download/score-excel")
	@SaCheckRole("super-admin")
	public void downloadExcel(HttpServletResponse response, String reviewStage) throws IOException {
		ReviewStageEnum fromValue = ReviewStageEnum.fromValue(reviewStage);
		paperManager.downloadScoreExcel(response, fromValue.getValue());
	}

	/** -----------------------以下跟分配審稿委員有關---------------------------------- */

	@Operation(summary = "為稿件新增/更新/刪除 複數 評審委員，要給予評審負責的審稿階段")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("assign-paper-reviewer")
	public R<Void> assignPaperReviewerToPaper(
			@Validated @RequestBody AddPaperReviewerToPaperDTO addPaperReviewerToPaperDTO) {

		// 先校驗是否跟Enum中的值一致
		ReviewStageEnum reviewStageEnum = ReviewStageEnum.fromValue(addPaperReviewerToPaperDTO.getReviewStage());

		paperReviewerManager.assignPaperReviewerToPaper(reviewStageEnum.getValue(),
				addPaperReviewerToPaperDTO.getTargetPaperReviewerIdList(), addPaperReviewerToPaperDTO.getPaperId());
		return R.ok();

	}

	@Operation(summary = "為稿件 自動 新增/更新/刪除 複數 評審委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("assign-paper-reviewer-auto")
	public R<Void> assignPaperReviewerToPaperAuto(@RequestBody @Valid ReviewStageDTO reviewStageDTO) {

		// 先校驗是否跟Enum中的值一致
		ReviewStageEnum reviewStageEnum = ReviewStageEnum.fromValue(reviewStageDTO.getReviewStage());

		// 帶著 階段值 進入自動
		paperReviewerManager.autoAssignPaperReviewer(reviewStageEnum.getValue());
		return R.ok();

	}

	/** ---------------第二階段 入選後上傳slide、poster、video API ------------------ */

	@GetMapping("owner/second-stage/{id}")
	@Operation(summary = "第二階段，查看此稿件上傳的檔案列表")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<List<PaperFileUpload>> getSecondStagePaperFile(@PathVariable("id") Long paperId) {

		// 1.根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		// 2.透過 paperId 和 memberId 去獲取此稿件在第二階段上傳的所有附件
		List<PaperFileUpload> secondStagePaperFile = paperService.getSecondStagePaperFile(paperId,
				memberCache.getMemberId());

		return R.ok(secondStagePaperFile);
	}

	@GetMapping("owner/second-stage/check")
	@Operation(summary = "第二階段，查看slide/poster/video 是否已上傳過相同檔案")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<CheckFileVO> slideCheck(@RequestParam String sha256) {
		// 透過用戶檔案的sha256值，用來判斷是否傳送過，也是達到秒傳的功能
		CheckFileVO checkFile = sysChunkFileService.checkFile(sha256);
		return R.ok(checkFile);
	}

	@PostMapping(value = "owner/second-stage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "第二階段，slide/poster/video 檔案分片上傳", description = "請使用formData包裝,兩個key <br>"
			+ "1.data(value = DTO(json))<br>" + "2.file(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<ChunkResponseVO> slideUpload(@RequestPart("file") MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = AddSlideUploadDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		AddSlideUploadDTO slideUploadDTO = objectMapper.readValue(jsonData, AddSlideUploadDTO.class);

		// slide分片上傳
		paperService.uploadSlideChunk(slideUploadDTO, memberCache.getMemberId(), file);

		return R.ok();
	}

	@PutMapping(value = "owner/second-stage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "第二階段，更新slide/poster/video 檔案分片上傳", description = "請使用formData包裝,兩個key <br>"
			+ "1.data(value = DTO(json))<br>" + "2.file(value = binary)<br>"
			+ "knife4j Web 文檔顯示有問題, 真實傳輸方式為 「multipart/form-data」<br>"
			+ "請用 http://localhost:8080/swagger-ui/index.html 測試 ")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<ChunkResponseVO> slideUpdate(@RequestPart("file") MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = PutSlideUploadDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		PutSlideUploadDTO slideUpdateDTO = objectMapper.readValue(jsonData, PutSlideUploadDTO.class);

		// slide分片上傳更新
		paperService.updateSlideChunk(slideUpdateDTO, memberCache.getMemberId(), file);

		return R.ok();
	}

	@DeleteMapping("owner/second-stage/{id}")
	@Operation(summary = "第二階段，刪除單一稿件附件")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> removeSecondStagePaperFile(@PathVariable("id") Long paperId, @RequestParam Long paperFileUploadId) {

		// 1.根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		// 2.透過 paperId 和 memberId 是否為實際投稿者在操作稿件，並透過paperFileId 刪除 第二階段 上傳的附件檔案
		paperService.removeSecondStagePaperFile(paperId, memberCache.getMemberId(), paperFileUploadId);

		return R.ok();
	}

	/** ----------下載 第一階段 所有摘要----------- */

	@PostMapping("download/get-download-folder-url")
	@Operation(summary = "返回所有摘要(第一階段)的下載連結，For管理者")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<String> getDownloadFolderUrl() {
		// 身分驗證後，生成UUID作為key
		String key = "DownloadFolder:" + UUID.randomUUID().toString();
		// 使用 Redisson 將key設置到 Redis，並設定過期時間為10分鐘
		RBucket<String> bucket = redissonClient.getBucket(key);
		// 假設存一個有效標誌，可以根據實際需求調整
		bucket.set("paper", 10, TimeUnit.MINUTES);

		// 構建下載URL並返回
		String downloadUrl = "/paper/download/all-abstracts?key=" + key;
		return R.ok("操作成功", downloadUrl);

	}

	@GetMapping("download/all-abstracts")
	@Operation(summary = "下載所有摘要稿件(以流式傳輸zip檔)")
	public ResponseEntity<StreamingResponseBody> downloadFiles(@RequestParam String key) throws RedisKeyException {
		// 從URL中獲取key參數
		RBucket<String> bucket = redissonClient.getBucket(key);

		// 檢查key是否有效且未過期
		if (bucket.isExists() && bucket.get().equals("paper")) {

			// 校驗通過，刪除key
			bucket.delete();

			// key有效，進行下載操作
			String folderName = "paper/abstracts";
			return minioUtil.downloadFolderZipByStream(folderName);

		} else {
			// key無效或已過期，返回錯誤
			throw new RedisKeyException("key無效或已過期");
		}

		// -----------------------------------

		// Stream範例
		//		StreamingResponseBody responseBody = outputStream -> {
		//			// 在這裡生成數據並寫入 outputStream
		//			for (int i = 0; i < 1000000; i++) {
		//				outputStream.write(("Data line " + i + "\n").getBytes());
		//				outputStream.flush();
		//				
		//			}
		//		};
		//		
		//
		//		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=data.txt").body(responseBody);
		//
		//		

	}

}
