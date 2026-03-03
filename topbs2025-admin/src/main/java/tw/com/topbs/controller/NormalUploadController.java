package tw.com.topbs.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.utils.R;

/**
 * <p>
 * 上傳一般檔案 前端控制器
 * </p>
 *
 * @author Joey
 * @since 2024-07-15
 */
@Tag(name = "上傳一般檔案API")
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/upload")
public class NormalUploadController {

	@Operation(summary = "上傳圖檔-反饋(無實際上傳)")
	@Parameters({
			@Parameter(name = "Authorization", description = "請求頭token,token-value開頭必須為Bearer ", required = true, in = ParameterIn.HEADER) })
	@PostMapping("img")
	public R<Map<String, Object>> uploadContentImg(MultipartFile file) {

		System.out.println("獲得檔案");

		HashMap<String, Object> hashMap = new HashMap<>();

		hashMap.put("url", "https://miro.medium.com/v2/resize:fit:582/1*4j2A9niz0eq-mRaCPUffpg.png");

		return R.ok("獲得檔案", hashMap);

	}

}
