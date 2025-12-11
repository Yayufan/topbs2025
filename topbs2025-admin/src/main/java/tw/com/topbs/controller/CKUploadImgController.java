package tw.com.topbs.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import tw.com.topbs.system.pojo.DTO.ChunkUploadDTO;
import tw.com.topbs.system.pojo.VO.ChunkResponseVO;
import tw.com.topbs.system.service.SysChunkFileService;
import tw.com.topbs.utils.R;
import tw.com.topbs.utils.S3Util;

/**
 * <p>
 * CKEditor 上傳圖檔 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2024-07-15
 */
@Tag(name = "CKEditor 上傳圖檔API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/ck")
public class CKUploadImgController {

	private final S3Util s3Util;
	private final SysChunkFileService chunkFileService;

	@Value("${spring.cloud.aws.s3.bucketName}")
	private String bucketName;

	@Operation(summary = "上傳CKEditor需要的檔案")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@PostMapping("upload-img")
	public Map<String, Object> uploadContentImg(@RequestParam("scope") String scope,
			@RequestParam("file") MultipartFile file) {

		String imgUrl = s3Util.upload(scope + "/",file.getOriginalFilename(), file);

		HashMap<String, Object> hashMap = new HashMap<>();
		hashMap.put("url", imgUrl);

		return hashMap;

	}

	/**
	 * S3Util 查看bucket是否存在
	 */
	@GetMapping("test/exist-bucket")
	public void existBucket(String bucketName) {
		s3Util.existBucket(bucketName);
	}

	@GetMapping("test/remove-bucket")
	public void removeBucket(String bucketName) {
		s3Util.removeBucket(bucketName);
	}

	@GetMapping("test/getFile")
	public void getFile(HttpServletResponse response, @RequestParam("filePath") String filePath)
			throws IOException, Exception {

		try (ResponseInputStream<GetObjectResponse> s3Stream = s3Util.getFile(filePath)) {

			// 從路徑中提取純檔名
			String filename = Paths.get(filePath).getFileName().toString();

			response.setContentType("application/octet-stream");
			response.setHeader("Content-Disposition",
					"attachment; filename=\"" + URLEncoder.encode(filename, StandardCharsets.UTF_8) + "\"");

			// 直接把 S3 的串流寫到 OutputStream，不進 JVM memory
			s3Stream.transferTo(response.getOutputStream());
		}
	}

	@GetMapping("test/getFileSize")
	public void getFileSize(String filePath) {
		long fileSize = s3Util.getFileSize(filePath);
		System.out.println("檔案大小為: " + fileSize + " bytes");
	}

	@GetMapping("test/totalFilesSize")
	public void totalFilesSize() {
		List<String> filesPath = Arrays.asList(
				"/topbs2025/paper/abstracts/Poster Presentation/pdf/Poster Presentation_ddddddddd_1750842463778.pdf",
				"/topbs2025/paper/abstracts/Poster Presentation/pdf/Poster Presentation_poster-author01_1750833730430.pdf");

		long calculateTotalSize = s3Util.calculateTotalSize(filesPath);
		System.out.println("總計大小:" + calculateTotalSize + " bytes");
	}

	@PostMapping(value = "test/upload01", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "預設的Bucket,上傳 單 檔案,自定義檔名")
	public void upload01(MultipartFile file) {
		String filePath = s3Util.upload("a/b/c", "ccc.pdf", file);
		System.out.println(filePath);
	}

	@PostMapping(value = "test/upload02", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "指定的Bucket(沒有則創建Bucket),上傳 單 檔案,自定義檔名")
	public void upload02(MultipartFile file, String bucketName) {
		String filePath = s3Util.upload("a/b/c", "ccc.pdf", file, bucketName);
		System.out.println(filePath);
	}

	@PostMapping(value = "test/upload03", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "預設的Bucket(沒有則創建Bucket),上傳 多 檔案")
	public void upload03(MultipartFile[] files) {
		List<String> filePaths = s3Util.upload("a/b/c", files);
		for (String filePath : filePaths) {
			System.out.println(filePath);
		}
	}

	@PostMapping(value = "test/upload04", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "指定的Bucket(沒有則創建Bucket),上傳 多 檔案")
	public void upload04(MultipartFile[] files, String bucketName) {
		List<String> filePaths = s3Util.upload("a/b/c", files, bucketName);
		for (String filePath : filePaths) {
			System.out.println(filePath);
		}
	}

	@GetMapping(value = "test/downloadFolder")
	@Operation(summary = "流式下載打包的壓縮檔")
	public ResponseEntity<StreamingResponseBody> downloadFolder(String folderName) {
		return s3Util.downloadFolderZipByStream(folderName, new HashMap<String, String>());
	}

	@GetMapping(value = "test/getFilePresignUrl")
	@Operation(summary = "獲得檔案的預簽名URL")
	public void getFilePresignUrl(String filePath) {
		String filePresignUrl = s3Util.getFilePresignUrl(filePath);
		System.out.println(filePresignUrl);
	}

	@GetMapping(value = "test/listObject")
	@Operation(summary = "所有檔案路徑")
	public void getListObejct() {
		s3Util.listObjects("topbs2025", "/paper/");
	}

	@DeleteMapping(value = "test/deleteObject")
	@Operation(summary = "刪除單個檔案")
	public void deleteObject() {
		s3Util.removeFile("topbs2025", "a/b/c/52MB 檔案_1765350602051.mp4");
	}

	@DeleteMapping(value = "test/deleteFolder")
	@Operation(summary = "刪除資料夾內所有檔案")
	public void deleteFolder() {
		s3Util.removeFolder("topbs2025", "a");
	}

	@PostMapping(value = "test/slideUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "分片上傳")
	public R<ChunkResponseVO> slideUpload(@RequestPart("file") MultipartFile file,
			@RequestPart("data") @Schema(name = "data", implementation = ChunkUploadDTO.class) String jsonData)
			throws JsonMappingException, JsonProcessingException {

		// 將 JSON 字符串轉為對象
		ObjectMapper objectMapper = new ObjectMapper();
		ChunkUploadDTO chunkUploadDTO = objectMapper.readValue(jsonData, ChunkUploadDTO.class);

		// slide分片上傳
		ChunkResponseVO chunkResponseVO = chunkFileService.uploadChunkS3(file, "a/b/c/d/", chunkUploadDTO);

		System.out.println(chunkResponseVO);

		return R.ok();
	}

}
