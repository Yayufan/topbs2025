package tw.com.topbs.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.exception.RedisKeyException;
import tw.com.topbs.pojo.DTO.PutPaperForAdminDTO;
import tw.com.topbs.pojo.DTO.SendEmailByTagDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerToPaperDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddTagToPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
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

	private final MinioUtil minioUtil;

	@GetMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "查詢單一稿件For後台")
	@SaCheckRole("super-admin")
	public R<PaperVO> getPaper(@PathVariable("id") Long paperId) {
		PaperVO vo = paperService.getPaper(paperId);
		return R.ok(vo);
	}

	@GetMapping("owner/{id}")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "查詢會員自身的單一稿件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<PaperVO> getPaperForOwner(@PathVariable("id") Long paperId) {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();
		PaperVO vo = paperService.getPaper(paperId, memberCache.getMemberId());
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
		List<PaperVO> voList = paperService.getPaperList(memberCache.getMemberId());
		return R.ok(voList);
	}

	@GetMapping("pagination")
	@Operation(summary = "查詢全部稿件(分頁)For後台管理")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<PaperVO>> getPaperPage(@RequestParam Integer page, @RequestParam Integer size,
			@RequestParam(required = false) String queryText, @RequestParam(required = false) Integer status,
			@RequestParam(required = false) String absType, @RequestParam(required = false) String absProp) {
		Page<Paper> pageable = new Page<Paper>(page, size);
		IPage<PaperVO> voPage = paperService.getPaperPage(pageable, queryText, status, absType, absProp);
		return R.ok(voPage);
	}

	@PostMapping
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "data", description = "JSON 格式的檔案資料", required = true, in = ParameterIn.QUERY, schema = @Schema(implementation = AddPaperDTO.class)) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Operation(summary = "新增單一稿件")
	public R<Void> savePaper(@RequestParam("file") MultipartFile[] files, @RequestParam("data") String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		// 處理Java 8 LocalDate 和 LocalDateTime的轉換
		objectMapper.registerModule(new JavaTimeModule());
		AddPaperDTO addPaperDTO = objectMapper.readValue(jsonData, AddPaperDTO.class);

		// 將檔案和資料對象傳給後端
		paperService.addPaper(files, addPaperDTO);

		return R.ok();
	}

	@PutMapping("owner")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "data", description = "JSON 格式的檔案資料", required = true, in = ParameterIn.QUERY, schema = @Schema(implementation = PutPaperDTO.class)) })
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	@Operation(summary = "修改單一稿件")
	public R<Void> updatePaper(@RequestParam("file") MultipartFile[] files, @RequestParam("data") String jsonData)
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
			paperService.updatePaper(files, putPaperDTO);
			return R.ok();
		} else {
			return R.fail(
					"Please do not maliciously tamper with other people's information. Legal measures will be taken after verification.");
		}

	}

	@PutMapping
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@Operation(summary = "修改單一稿件For管理者")
	public R<Void> updatePaperForAdmin(@RequestBody @Valid PutPaperForAdminDTO putPaperForAdminDTO) {
		paperService.updatePaperForAdmin(putPaperForAdminDTO);
		return R.ok();
	}

	@DeleteMapping("{id}")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除單一稿件For後台")
	@SaCheckRole("super-admin")
	public R<Void> deletePaper(@PathVariable("id") Long paperId) {
		paperService.deletePaper(paperId);
		return R.ok();
	}

	@DeleteMapping("owner/{id}")
	@Parameters({
			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@Operation(summary = "刪除會員自身的單一稿件")
	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<Void> deletePaperForOwner(@PathVariable("id") Long paperId) {
		// 根據token 拿取本人的數據
		Member memberCache = memberService.getMemberInfo();

		paperService.deletePaper(paperId, memberCache.getMemberId());
		return R.ok();
	}

	@DeleteMapping
	@Operation(summary = "批量刪除稿件For後台")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeletePaper(@RequestBody List<Long> paperIds) {
		paperService.deletePaperList(paperIds);
		return R.ok();

	}

	@Operation(summary = "為稿件新增/更新/刪除 複數 評審委員")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("assign-paper-reviewer")
	public R<Void> assignPaperReviewerToPaper(
			@Validated @RequestBody AddPaperReviewerToPaperDTO addPaperReviewerToPaperDTO) {
		paperService.assignPaperReviewerToPaper(addPaperReviewerToPaperDTO.getTargetPaperReviewerIdList(),
				addPaperReviewerToPaperDTO.getPaperId());
		return R.ok();

	}

	@Operation(summary = "為稿件新增/更新/刪除 複數標籤")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PutMapping("tag")
	public R<Void> assignTagToPaper(@Validated @RequestBody AddTagToPaperDTO addTagToPaperDTO) {
		paperService.assignTagToPaper(addTagToPaperDTO.getTargetTagIdList(), addTagToPaperDTO.getPaperId());
		return R.ok();
	}

	/** 以下與寄送給通訊作者信件有關 */
	@Operation(summary = "寄送信件給通訊作者(稿件)，可根據tag來篩選寄送")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	@PostMapping("send-email")
	public R<Void> sendEmailToCorrespondingAuthor(@Validated @RequestBody SendEmailByTagDTO sendEmailByTagDTO) {
		paperService.sendEmailToPapers(sendEmailByTagDTO.getTagIdList(), sendEmailByTagDTO.getSendEmailDTO());
		return R.ok();

	}

	@GetMapping("slide-check")
	@Operation(summary = "查看slide 或 video 是否已上傳過相同檔案")
	//	@Parameters({
	//		@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	//	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<CheckFileVO> slideCheck(@RequestParam String sha256) {
		// 根據token 拿取本人的數據
		// Member memberCache = memberService.getMemberInfo();

		// 透過用戶檔案的sha256值，用來判斷是否傳送過，也是達到秒傳的功能
		CheckFileVO checkFile = sysChunkFileService.checkFile(sha256);
		return R.ok(checkFile);
	}

	@PostMapping("slide-upload")
	@Operation(summary = "大檔案slide 或 video的分片上傳")
	@Parameters({
			//			@Parameter(name = "Authorization-member", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER),
			@Parameter(name = "data", description = "JSON 格式的檔案資料", required = true, in = ParameterIn.QUERY, schema = @Schema(implementation = ChunkUploadDTO.class)) })
	//	@SaCheckLogin(type = StpKit.MEMBER_TYPE)
	public R<ChunkResponseVO> slideUpload(@RequestParam("file") MultipartFile file,
			@RequestParam("data") String jsonData) throws JsonMappingException, JsonProcessingException {
		// 根據token 拿取本人的數據
		// Member memberCache = memberService.getMemberInfo();

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		ChunkUploadDTO chunkUploadDTO = objectMapper.readValue(jsonData, ChunkUploadDTO.class);

		// 分片上傳
		ChunkResponseVO uploadChunk = sysChunkFileService.uploadChunk(file, chunkUploadDTO);

		return R.ok(uploadChunk);
	}

	@PostMapping("get-download-folder-url")
	@Operation(summary = "返回Folder的下載連結 For管理者")
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
		String downloadUrl = "/paper/download-all-abstructs?key=" + key;
		return R.ok("操作成功", downloadUrl);

	}

	@GetMapping("download-all-abstructs")
	@Operation(summary = "以流式傳輸zip檔，下載所有摘要稿件，")
	public ResponseEntity<StreamingResponseBody> downloadFiles(@RequestParam String key) throws RedisKeyException {
		// 從URL中獲取key參數
		RBucket<String> bucket = redissonClient.getBucket(key);

		// 檢查key是否有效且未過期
		if (bucket.isExists() && bucket.get().equals("paper")) {

			// 校驗通過，刪除key
			bucket.delete();

			// key有效，進行下載操作
			String folderName = "paper/abstructs";
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
