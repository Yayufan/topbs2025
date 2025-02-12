package tw.com.topbs.service;

import java.util.List;

/**
 * 獨立的方法,用於處理本次HTML Content更新
 */
public interface CmsService {

	/**
	 * 不考慮醫療新知 和 最新消息同步於其他文章情況下， 更新數據前使用,這能去解析
	 * 新/舊HTML，以及前端傳來臨時檔案的URL,哪些舊的圖片沒在使用,會去從Minio中移除
	 * 
	 * 
	 * @param newHtml
	 * @param oldHtml
	 * @param tempUploadUrl
	 * @param bucketName
	 */
	void cleanNotUsedImg(String newHtml, String oldHtml, List<String> tempUploadUrl, String bucketName);

	/**
	 * 考慮醫療新知 和 最新消息同步於其他文章情況下， 更新數據前使用,這能去解析
	 * 新HTML、舊HTML、以及同步的文章、以及前端傳來臨時檔案的URL,哪些圖片沒在使用,會去從Minio中移除
	 * 
	 * 
	 * @param newHtml
	 * @param oldHtml
	 * @param asyncArticelHtml
	 * @param tempUploadUrl
	 * @param bucketName
	 */
	void cleanNotUsedImg(String newHtml, String oldHtml, String asyncArticelHtml, List<String> tempUploadUrl,
			String bucketName);

	/**
	 * 刪除數據前使用,這能去解析 舊HTML中,使用了哪些圖片, 這些要從Minio中移除
	 * 
	 * @param oldHtml
	 * @param bucketName
	 */
	void cleanNotUsedImg(String oldHtml, String bucketName);

}
