package tw.com.topbs.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperDTO;
import tw.com.topbs.pojo.VO.PaperVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.entity.Paper;
import tw.com.topbs.saToken.StpKit;
import tw.com.topbs.service.MemberService;
import tw.com.topbs.service.PaperService;
import tw.com.topbs.utils.MinioUtil;
import tw.com.topbs.utils.R;

@Tag(name = "稿件API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/paper")
public class PaperController {

	private final PaperService paperService;
	private final MemberService memberService;

	private final MinioUtil minioUtil;

	// MinioClient对象，用于与MinIO服务进行交互
	private final MinioClient minioClient;

	// 預設存储桶名称
	@Value("${minio.bucketName}")
	private String bucketName;

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
	@Operation(summary = "查詢全部稿件(分頁)For 後台管理")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<IPage<PaperVO>> getUserPage(@RequestParam Integer page, @RequestParam Integer size) {
		Page<Paper> pageable = new Page<Paper>(page, size);
		IPage<PaperVO> voPage = paperService.getPaperPage(pageable);
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
	public R<Paper> updatePaper(@RequestParam("file") MultipartFile[] files, @RequestParam("data") String jsonData)
			throws JsonMappingException, JsonProcessingException {
		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
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
	@Operation(summary = "批量刪除稿件For 後台")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckRole("super-admin")
	public R<Void> batchDeletePaper(@RequestBody List<Long> paperIds) {
		paperService.deletePaperList(paperIds);
		return R.ok();

	}

	@GetMapping("download-all-abstructs")
	@Operation(summary = "下載所有摘要稿件")
	public ResponseEntity<StreamingResponseBody> downloadFiles(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		System.out.println("觸發");

		String folderName = "paper";

		return minioUtil.downloadFolderStream(request, response, folderName);

//		StreamingResponseBody responseBody = outputStream -> {
//			// 在這裡生成數據並寫入 outputStream
//
//			// 創建zip的OutputStream
//			try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
//
//				// 從minio獲取資料夾內的檔案列表
//				List<ObjectItem> listObjects = minioUtil.listObjects(bucketName, folderName);
//
//				for (ObjectItem objectItem : listObjects) {
//					System.out.println("物件名為: " + objectItem.getObjectName());
//					System.out.println("物件Size為: " + objectItem.getSize() + " byte");
//
//					// Preserve original path structure within ZIP ， 跟objectName 只差在 有沒有paper/ 這層
//					String relativePath = objectItem.getObjectName().substring(folderName.length() + 1);
//
//					// Create ZIP entry
//					ZipEntry zipEntry = new ZipEntry(relativePath);
//					zipOut.putNextEntry(zipEntry);
//
//					try {
//
//						// 獲取物件的inputStream流
//						InputStream in = minioClient.getObject(
//								GetObjectArgs.builder().bucket(bucketName).object(objectItem.getObjectName()).build());
//
//						byte[] buffer = new byte[4096];
//						int bytesRead;
//
//						while ((bytesRead = in.read(buffer)) != -1) {
//							zipOut.write(buffer, 0, bytesRead);
//							zipOut.flush();
//						}
//						;
//
//						zipOut.closeEntry();
//
//					} catch (Exception e) {
//						// TODO: handle exception
//					}
//
//				}
//
//			} catch (Exception e) {
//				// TODO: handle exception
//				e.printStackTrace();
//			}
//
//		};
//
//		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=paper.zip").body(responseBody);
//
//		

		// -----------------------------------

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
