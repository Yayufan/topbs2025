package tw.com.topbs.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.service.CmsService;
import tw.com.topbs.utils.JsoupUtil;
import tw.com.topbs.utils.MinioUtil;

@RequiredArgsConstructor
@Service
public class CmsServiceImpl implements CmsService {

	private final MinioUtil minioUtil;

	@Override
	public void cleanNotUsedImg(String newHtml, String oldHtml, List<String> tempUploadUrl, String bucketName) {
		// 透過封裝的JsoupUtil 提取 img標籤內的src屬性,變成一個URL List
		List<String> contentImageUrls = JsoupUtil.extractImageUrls(newHtml);

		// 透過封裝的JsoupUtil 提取原本HTML內容 img標籤內的src屬性,變成一個URL List
		List<String> originalImageUrls = JsoupUtil.extractImageUrls(oldHtml);

		// 過濾出那些在 originalImageUrls 中存在，但不在 contentImageUrls 中的 URL。
		List<String> unUsedContentImg = originalImageUrls.stream().filter(url -> !contentImageUrls.contains(url))
				.collect(Collectors.toList());

		// 因為在圖片路徑是含http協議和域名的,所以這邊要進行提取
		List<String> unUsedMinioPaths = minioUtil.extractPaths(bucketName, unUsedContentImg);

		System.out.println("要移除的舊路徑" + unUsedMinioPaths);

		// 移除minio中的圖片
		minioUtil.removeObjects(bucketName, unUsedMinioPaths);

		// ----------------------處理臨時文件-----------------------------------
		// 過濾出那些在 tempUploadUrl 中存在，但不在 contentImageUrls 中的 URL。
		List<String> unUsedTempImg = tempUploadUrl.stream().filter(url -> !contentImageUrls.contains(url))
				.collect(Collectors.toList());

		// 因為在圖片路徑是含http協議和域名的,所以這邊要進行提取
		List<String> unUsedTempMinioPaths = minioUtil.extractPaths(bucketName, unUsedTempImg);

		System.out.println("要移除的臨時文件路徑" + unUsedTempMinioPaths);

		// 移除minio中的圖片
		minioUtil.removeObjects(bucketName, unUsedTempMinioPaths);

	}

	@Override
	public void cleanNotUsedImg(String newHtml, String oldHtml, String asyncArticelHtml, List<String> tempUploadUrl,
			String bucketName) {
		// TODO Auto-generated method stub
		// 透過封裝的JsoupUtil 提取 img標籤內的src屬性,變成一個URL List
		List<String> contentImageUrls = JsoupUtil.extractImageUrls(newHtml);

		// 透過封裝的JsoupUtil 提取原本HTML內容 img標籤內的src屬性,變成一個URL List
		List<String> originalImageUrls = JsoupUtil.extractImageUrls(oldHtml);

		// 透過封裝的JsoupUtil 提取同步文章的HTML內容 img標籤內的src屬性,變成一個URL List
		List<String> asyncImageUrls = JsoupUtil.extractImageUrls(asyncArticelHtml);

		// 透過originalImageUrls創建一個新的 List 来儲存結果
		List<String> unUsedContentImg = new ArrayList<>(originalImageUrls);

		// 從 urlsToRemove 中移除所有在 contentImageUrls 中存在的 URL
		unUsedContentImg.removeAll(contentImageUrls);

		// 從 urlsToRemove 中移除所有在 asyncImageUrls 中存在的 URL
		unUsedContentImg.removeAll(asyncImageUrls);

		// 因為在圖片路徑是含http協議和域名的,所以這邊要進行提取
		List<String> unUsedMinioPaths = minioUtil.extractPaths(bucketName, unUsedContentImg);

		System.out.println("保留新內容和同步文章的圖片，要移除的舊圖片路徑有:" + unUsedMinioPaths);

		// 移除minio中的圖片
		minioUtil.removeObjects(bucketName, unUsedMinioPaths);
		
		
		// ----------------------處理臨時文件-----------------------------------
		// 過濾出那些在 tempUploadUrl 中存在，但不在 contentImageUrls 中的 URL。
		List<String> unUsedTempImg = tempUploadUrl.stream().filter(url -> !contentImageUrls.contains(url))
				.collect(Collectors.toList());

		// 因為在圖片路徑是含http協議和域名的,所以這邊要進行提取
		List<String> unUsedTempMinioPaths = minioUtil.extractPaths(bucketName, unUsedTempImg);

		System.out.println("要移除的臨時文件路徑:" + unUsedTempMinioPaths);

		// 移除minio中的圖片
		minioUtil.removeObjects(bucketName, unUsedTempMinioPaths);

	}

	@Override
	public void cleanNotUsedImg(String oldHtml, String bucketName) {
		// TODO Auto-generated method stub
		// 透過封裝的JsoupUtil 提取原本HTML內容 img標籤內的src屬性,變成一個URL List
		List<String> originalImageUrls = JsoupUtil.extractImageUrls(oldHtml);

		// 因為在圖片路徑是含http協議和域名的,所以這邊要進行提取
		List<String> unUsedMinioPaths = minioUtil.extractPaths(bucketName, originalImageUrls);

		// 移除minio中的圖片
		minioUtil.removeObjects(bucketName, unUsedMinioPaths);

	}

}
