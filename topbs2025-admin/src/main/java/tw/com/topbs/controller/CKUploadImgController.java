package tw.com.topbs.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.utils.MinioUtil;

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

	private final MinioUtil minioUtil;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	@Operation(summary = "上傳CKEditor需要的檔案")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@SaCheckLogin
	@PostMapping("upload-img")
	public Map<String, Object> uploadContentImg(@RequestParam("scope") String scope,
			@RequestParam("file") MultipartFile[] file) {

		// 調用封裝好的工具類,獲得一個字符串List
		List<String> imgUrlList = minioUtil.upload(minioBucketName, scope + "/", file);
		// 因為CKEditor upload都是單個圖檔,所以這邊一定只有一個元素
		String imgUrl = imgUrlList.get(0);

		// 組裝返回給前端
		HashMap<String, Object> hashMap = new HashMap<>();
		// hashMap.put("url",
		// "https://miro.medium.com/v2/resize:fit:582/1*4j2A9niz0eq-mRaCPUffpg.png");
		imgUrl = "/" + minioBucketName + "/" + imgUrl;
		System.out.println(imgUrl);

		hashMap.put("url", imgUrl);

		return hashMap;

	}

}
