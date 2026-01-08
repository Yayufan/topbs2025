package tw.com.topbs.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// AWS S3 SDK V2 依赖
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import tw.com.topbs.pojo.sys.ObjectItem;

/**
 * @description： S3工具类 @version：1.0
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Util {

	// 私有静态常量：用于创建公共读取策略的 JSON 模板
	private static final String PUBLIC_READ_POLICY_TEMPLATE = """
			{
			  "Version": "2012-10-17",
			  "Statement": [
			    {
			      "Sid": "PublicReadGetObject",
			      "Effect": "Allow",
			      "Principal": "*",
			      "Action": [
			        "s3:GetObject"
			      ],
			      "Resource": [
			        "arn:aws:s3:::%s/*"
			      ]
			    }
			  ]
			}
			""";

	// S3Client对象，用于与S3兼容服务进行交互
	private final S3Client s3Client;
	// S3Presigner对象，用于生成预签名URL
	private final S3Presigner s3Presigner;

	@Qualifier("taskExecutor") // 使用您配置的線程池
	private final Executor taskExecutor;

	// 「預設」存储桶名称
	@Value("${spring.cloud.aws.s3.bucketName}") // 注意：这里的 Value key 可能需要对应您的配置
	private String bucketName;

	/**
	 * 判斷是否存在，不存在則創建
	 *
	 * @return: void
	 */
	public void existBucket(String name) {
		try {
			// 1.檢查Bucket是否存在 (使用 headBucket，如果存在返回 200，否则抛出 NoSuchBucketException)
			s3Client.headBucket(HeadBucketRequest.builder().bucket(name).build());
			return;
		} catch (NoSuchBucketException e) {
			log.info("Bucket {} 不存在，正在創建...", name);
			try {
				// 2.如果不存在則創建桶
				s3Client.createBucket(CreateBucketRequest.builder().bucket(name).build());
				// 3.靜態模板進行格式化 - 共用瀏覽權限
				String publicReadPolicy = String.format(PUBLIC_READ_POLICY_TEMPLATE, name);

				// 4. 设置 Bucket 共用Policy
				PutBucketPolicyRequest putPolicyRequest = PutBucketPolicyRequest.builder()
						.bucket(name)
						.policy(publicReadPolicy)
						.build();

				s3Client.putBucketPolicy(putPolicyRequest);
				log.info("Bucket {} 創建成功，並已設置為 Public Read 權限。", name);

			} catch (BucketAlreadyOwnedByYouException ignored) {
				// 忽略已存在 , 但是屬於自己的異常
			} catch (S3Exception ex) {
				log.error("創建 Bucket {} 失敗: {}", name, ex.getMessage());
				ex.printStackTrace();
			}
		} catch (S3Exception e) {
			log.error("檢查 Bucket {} 狀態失敗: {}", name, e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 刪除存儲bucket，空桶時才能刪除
	 *
	 * @param bucketName 存儲bucket名稱
	 * @return Boolean
	 */
	public Boolean removeBucket(String bucketName) {
		try {
			// 删除存储桶
			s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
		} catch (S3Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 從 「「預設」」 Bucket 取檔案
	 * 
	 * @param filePath , DB中的filePath會包含bucket, 例:bucketName/paper/aaa.pdf
	 * @return
	 * @throws Exception
	 */
	public ResponseInputStream<GetObjectResponse> getFile(String filePath) throws Exception {
		return getFileInternal(filePath, bucketName);
	}

	/**
	 * 從 「「指定」」 Bucket 取檔案
	 * 
	 * @param filePath   , DB中的filePath會包含bucket, 例:bucketName/paper/aaa.pdf
	 * @param bucketName
	 * @return
	 * @throws Exception
	 */
	public ResponseInputStream<GetObjectResponse> getFile(String filePath, String bucketName) throws Exception {
		return getFileInternal(filePath, bucketName);
	}

	/**
	 * 共用邏輯：從 DB 的filePath , 抽取在S3 中儲存的路徑<br>
	 * 最後返回一個InputStream
	 * 
	 * @param filePath , DB中的filePath會包含bucket, 例:bucketName/paper/aaa.pdf
	 * @param bucket
	 * @return
	 * @throws Exception
	 */
	private ResponseInputStream<GetObjectResponse> getFileInternal(String filePath, String bucket) throws Exception {
		String filePathInS3 = this.extractS3PathInDbUrl(bucket, filePath);
		GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(filePathInS3).build();

		try {
			return s3Client.getObject(request);

		} catch (NoSuchKeyException e) {
			log.error("S3 找不到檔案: bucket={}, key={}", bucket, filePathInS3);
			throw new IOException("File not found in S3.", e);

		} catch (Exception e) {
			log.error("S3 取得檔案失敗: {}", e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * 從 「預設」 Bucket 取檔案 size
	 * 
	 * @param filePath
	 * @return
	 */
	public long getFileSize(String filePath) {
		return getFileSizeInternal(filePath, bucketName);
	}

	/**
	 * 從 「指定」 Bucket 取檔案 size
	 * 
	 * @param filePath
	 * @param bucketName
	 * @return
	 */
	public long getFileSize(String filePath, String bucketName) {
		return getFileSizeInternal(filePath, bucketName);
	}

	/**
	 * 共用邏輯：組 key、建 request、處理例外
	 * 
	 * @param filePath
	 * @param bucket
	 * @return
	 */
	private long getFileSizeInternal(String filePath, String bucket) {
		if (filePath == null) {
			return 0L;
		}

		String normalizeFilePath = this.normalizeFilePath(filePath);

		try {
			String filePathInS3 = this.extractS3PathInDbUrl(bucket, normalizeFilePath);

			HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(filePathInS3).build();

			HeadObjectResponse response = s3Client.headObject(request);
			return response.contentLength();

		} catch (NoSuchKeyException e) {
			log.warn("檔案不存在，無法獲取大小: {}", filePath);
			return 0L;

		} catch (S3Exception e) {
			log.error("S3 headObject 失敗: {}", e.getMessage(), e);
			return 0L;
		}
	}

	/**
	 * 是用於小檔案 20MB以內 , 通常是E-Mail中的附件使用
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public byte[] getFileBytes(String path) throws IOException {

		String normalizeFilePath = this.normalizeFilePath(path);

		GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(normalizeFilePath).build();

		try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(request);
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			byte[] buffer = new byte[8192];
			int bytesRead;

			while ((bytesRead = s3Object.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
			}

			return output.toByteArray();
		}
	}

	/**
	 * 給予一個檔案路徑列表 , 從「預設」bucket中查找<br>
	 * 並返回所有檔案相加的大小
	 * 
	 * @param filePaths
	 * @return
	 */
	public long calculateTotalSize(List<String> filePaths) {
		return this.calculateTotalSizeInternal(filePaths, bucketName);
	}

	/**
	 * 給予一個檔案路徑列表 , 從 「指定」 bucket中查找<br>
	 * 並返回所有檔案相加的大小
	 * 
	 * @param filePaths
	 * @param bucketName
	 * @return
	 */
	public long calculateTotalSize(List<String> filePaths, String bucketName) {
		return this.calculateTotalSizeInternal(filePaths, bucketName);
	}

	/**
	 * 共用邏輯：給予一個檔案路徑列表 , 從 目標 bucket中查找<br>
	 * 並返回所有檔案相加的大小
	 * 
	 * @param filePaths
	 * @param bucket
	 * @return
	 */
	private long calculateTotalSizeInternal(List<String> filePaths, String bucket) {
		long totalSize = 0L;

		if (filePaths == null || filePaths.isEmpty()) {
			return totalSize;
		}

		for (String filePath : filePaths) {
			if (filePath == null) {
				continue;
			}

			String normalizeFilePath = this.normalizeFilePath(filePath);

			try {
				// 提取
				String s3Key = this.extractS3PathInDbUrl(bucket, normalizeFilePath);

				// 檔案元數組的請求組裝
				HeadObjectRequest request = HeadObjectRequest.builder().bucket(bucket).key(s3Key).build();

				// 檔案元數據的結果
				HeadObjectResponse response = s3Client.headObject(request);

				// 檔案元數據的Size
				totalSize += response.contentLength();

			} catch (NoSuchKeyException e) {
				log.warn("檔案不存在，略過計算大小: {}", filePath);

			} catch (S3Exception e) {
				log.error("S3 headObject 失敗: {}", e.getMessage(), e);
			}
		}

		return totalSize;
	}

	/**
	 * 單檔案上傳，在 「預設」 的Bucket中儲存檔案<br>
	 * 建議在100MB內檔案使用
	 * 
	 * @param path          儲存的路徑
	 * @param fileName      自定義檔名
	 * @param multipartFile 檔案本身
	 * @return
	 */
	public String upload(String path, String fileName, MultipartFile multipartFile) {
		return this.uploadSingleInternal(path, fileName, multipartFile, bucketName);
	}

	/**
	 * 單檔案上傳，在 「指定」 的Bucket中儲存檔案
	 * 建議在100MB內檔案使用
	 * 
	 * @param path          儲存的路徑
	 * @param fileName      自定義檔名
	 * @param multipartFile 檔案本身
	 * @param bucketName    「指定」的儲存桶
	 * @return
	 */
	public String upload(String path, String fileName, MultipartFile multipartFile, String bucketName) {
		return this.uploadSingleInternal(path, fileName, multipartFile, bucketName);
	}

	/**
	 * 多檔案上傳，在 「預設」 的Bucket中儲存檔案
	 * 建議在100MB內檔案使用
	 * 
	 * @param path           儲存的路徑
	 * @param multipartFiles 檔案本身(複數)
	 * @return
	 */
	public List<String> upload(String path, MultipartFile[] multipartFiles) {
		return this.uploadMultipleInternal(path, multipartFiles, bucketName);
	}

	/**
	 * 多檔案上傳，在 「指定」 的Bucket中儲存檔案
	 * 建議在100MB內檔案使用
	 * 
	 * @param path           儲存的路徑
	 * @param multipartFiles 檔案本身(複數)
	 * @param bucketName     「指定」的儲存桶
	 * @return
	 */
	public List<String> upload(String path, MultipartFile[] multipartFiles, String bucketName) {
		return this.uploadMultipleInternal(path, multipartFiles, bucketName);
	}

	/**
	 * 單檔案上傳 內部方法
	 * 
	 * @param path       基本路徑
	 * @param fileName   自定義檔名
	 * @param file       檔案
	 * @param bucketName 儲存桶
	 * @return
	 */
	private String uploadSingleInternal(String path, String fileName, MultipartFile file, String bucketName) {
		// 1.先看是否有這個Bucket , 沒有則創建Public權限的Bucket
		this.existBucket(bucketName);

		// 2.規範化儲存路徑
		String normalizedPath = this.normalizePath(path);

		// 3.拆檔名 和 副檔名
		FileNameParts nameParts = this.extractNameParts(fileName);

		// 4.檔名加上 timestamp
		String fullFileName = normalizedPath + nameParts.baseName + "_" + System.currentTimeMillis()
				+ nameParts.extension;

		// 5.將新檔名上傳到S3 , 並返回儲存在DB的path
		return this.uploadToS3(bucketName, fullFileName, file);

	}

	/**
	 * 多檔案上傳 內部方法
	 * 
	 * @param path       基本路徑
	 * @param files      檔案(複數)
	 * @param bucketName 儲存桶
	 * @return
	 */
	private List<String> uploadMultipleInternal(String path, MultipartFile[] files, String bucketName) {
		// 1.先看是否有這個Bucket , 沒有則創建Public權限的Bucket
		this.existBucket(bucketName);

		// 2.初始化儲存路徑列表
		List<String> filePaths = new ArrayList<>(files.length);

		// 3.遍歷多檔案
		for (MultipartFile file : files) {
			// 3-1 取得檔案的原始檔名
			String originalName = file.getOriginalFilename();
			// 3-2 如果檔名為null則跳過
			if (originalName == null) {
				continue;
			}

			// 3-3.規範化儲存路徑
			String normalizedPath = this.normalizePath(path);

			// 3-4.拆檔名 和 副檔名
			FileNameParts nameParts = this.extractNameParts(originalName);

			// 3-5.檔名加上時間戳,並產生完整DB儲存路徑
			String s3Key = normalizedPath + nameParts.baseName + "_" + System.currentTimeMillis() + nameParts.extension;

			// 3-6.將新檔名上傳到S3
			String filePathInDb = this.uploadToS3(bucketName, s3Key, file);

			// 3-7 將檔案路徑
			filePaths.add(filePathInDb);
		}

		// 4.返回所有路徑
		return filePaths;
	}

	/**
	 * 將檔案上傳到S3
	 * 
	 * @param bucket       儲存桶
	 * @param fullFileName 物件儲存位置,也就是S3中的key
	 * @param file         檔案本身
	 */
	private String uploadToS3(String bucket, String s3Key, MultipartFile file) {
		try (InputStream in = file.getInputStream()) {

			/**
			 * 只負責「描述」元數據
			 */
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.contentLength(file.getSize())
					.contentType(file.getContentType())
					.build();

			/**
			 * 只負責「提供檔案內容」
			 * S3 不能直接丟 InputStream 是 AWS SDK v2 的 API 設計本意。
			 * 這樣做的主要原因包括：
			 * 
			 * 1.InputStream 不可預知長度
			 * 2.InputStream 不可 repeat 讀取
			 * 3.需要可靠 retry
			 * 4.底層 I/O 抽象統一
			 */
			s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(in, file.getSize()));

			return this.formatDbUrl(bucket, s3Key);

		} catch (Exception e) {
			log.error("上傳檔案 {} 失敗: {}", s3Key, e.getMessage(), e);
			throw new RuntimeException("上傳檔案" + s3Key + "失敗");
		}

	}

	/*
	 * ============================================================
	 * 工具方法：拆 baseName / extension
	 * ============================================================
	 */
	private static class FileNameParts {
		String baseName;
		String extension;

		FileNameParts(String baseName, String extension) {
			this.baseName = baseName;
			this.extension = extension;
		}
	}

	/**
	 * 使用 「預設」 bucket 初始化 multipart upload 分片上傳
	 */
	public String initializeMultipartUpload(String s3Key, String contentType, Map<String, String> metadata) {
		return initializeMultipartUploadInternal(s3Key, contentType, metadata, bucketName);
	}

	/**
	 * 使用 「指定」 bucket 初始化 multipart upload 分片上傳
	 */
	public String initializeMultipartUpload(String s3Key, String contentType, Map<String, String> metadata,
			String bucketName) {
		return initializeMultipartUploadInternal(s3Key, contentType, metadata, bucketName);
	}

	/**
	 * 
	 * 共用初始化 S3 Multipart Upload
	 * 
	 * @param s3Key       S3 對象鍵
	 * @param contentType 文件類型
	 * @param metadata    元數據
	 * @param bucket
	 * @return
	 */
	private String initializeMultipartUploadInternal(String s3Key, String contentType, Map<String, String> metadata,
			String bucket) {
		try {
			CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.contentType(contentType)
					.metadata(metadata)
					.build();

			CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);
			String uploadId = response.uploadId();

			log.info("初始化 S3 Multipart Upload 成功: uploadId={}, s3Key={}", uploadId, s3Key);
			System.out.println("第一次分片上傳初始化 , uploadId為: " + uploadId);

			return uploadId;

		} catch (S3Exception e) {
			log.error("初始化 Multipart Upload 失敗: s3Key={}", s3Key, e);
			throw new RuntimeException("初始化 S3 上傳失敗", e);
		}
	}

	/**
	 * 使用「預設」 bucket 上傳分片
	 * 
	 * @param s3Key      S3 對象鍵
	 * @param uploadId   Multipart Upload ID
	 * @param partNumber 分片編號（從 1 開始）
	 * @param file       分片文件
	 * @return
	 */
	public String uploadPart(String s3Key, String uploadId, int partNumber, MultipartFile file) {
		return uploadPartInternal(s3Key, uploadId, partNumber, file, bucketName);
	}

	/**
	 * 使用「指定」 bucket 上傳分片
	 * 
	 * @param s3Key      S3 對象鍵
	 * @param uploadId   Multipart Upload ID
	 * @param partNumber 分片編號（從 1 開始）
	 * @param file       分片文件
	 * @param bucketName 儲存桶
	 * @return
	 */
	public String uploadPart(String s3Key, String uploadId, int partNumber, MultipartFile file, String bucketName) {
		return uploadPartInternal(s3Key, uploadId, partNumber, file, bucketName);
	}

	/**
	 * 共用分片上傳邏輯，上傳單個分片到 S3
	 * 
	 * @param s3Key      S3 對象鍵
	 * @param uploadId   Multipart Upload ID
	 * @param partNumber 分片編號（從 1 開始）
	 * @param file       分片文件
	 * @param bucket     儲存桶
	 * @return
	 */
	private String uploadPartInternal(String s3Key, String uploadId, int partNumber, MultipartFile file,
			String bucket) {

		try (InputStream in = file.getInputStream()) {

			UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.uploadId(uploadId)
					.partNumber(partNumber)
					.build();

			UploadPartResponse response = s3Client.uploadPart(uploadPartRequest,
					RequestBody.fromInputStream(in, file.getSize()));

			String eTag = response.eTag();
			log.debug("分片上傳成功: partNumber={}, eTag={}", partNumber, eTag);
			return eTag;

		} catch (Exception e) {
			log.error("上傳分片失敗: partNumber={}, uploadId={}", partNumber, uploadId, e);
			throw new RuntimeException("上傳分片失敗", e);
		}
	}

	/**
	 * 
	 * 「預設」 bucket 完成分片合併
	 * 
	 * @param s3Key    S3 對象鍵
	 * @param uploadId Multipart Upload ID
	 * @param parts    分片列表（partNumber 和 eTag）
	 * @return
	 */
	public String completeMultipartUpload(String s3Key, String uploadId, List<PartInfo> parts) {
		return completeMultipartUploadInternal(s3Key, uploadId, parts, bucketName);
	}

	/**
	 * 
	 * 「指定」 bucket 完成分片合併
	 * 
	 * @param s3Key      S3 對象鍵
	 * @param uploadId   Multipart Upload ID
	 * @param parts      分片列表（partNumber 和 eTag）
	 * @param bucketName 儲存桶
	 * @return
	 */
	public String completeMultipartUpload(String s3Key, String uploadId, List<PartInfo> parts, String bucketName) {
		return completeMultipartUploadInternal(s3Key, uploadId, parts, bucketName);
	}

	/**
	 * 共用分片合併邏輯，完成 S3 Multipart Upload（合併分片）
	 * 
	 * @param s3Key    S3 對象鍵
	 * @param uploadId Multipart Upload ID
	 * @param parts    分片列表（partNumber 和 eTag）
	 * @param bucket   儲存桶
	 * @return
	 */
	private String completeMultipartUploadInternal(String s3Key, String uploadId, List<PartInfo> parts, String bucket) {
		try {
			// 排序 partNumber
			List<PartInfo> sortedParts = new ArrayList<>(parts);
			sortedParts.sort(Comparator.comparingInt(PartInfo::getPartNumber));

			// 轉成 CompletedPart
			List<CompletedPart> completedParts = sortedParts.stream()
					.map(p -> CompletedPart.builder().partNumber(p.getPartNumber()).eTag(p.getETag()).build())
					.collect(Collectors.toList());

			CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder().parts(completedParts).build();

			CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.uploadId(uploadId)
					.multipartUpload(completedUpload)
					.build();

			CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);

			log.info("S3 分片合併完成: s3Key={}, location={}", s3Key, response.location());
			return response.location();

		} catch (S3Exception e) {
			log.error("完成 Multipart Upload 失敗: uploadId={}, s3Key={}", uploadId, s3Key, e);
			throw new RuntimeException("合併分片失敗", e);
		}
	}

	/**
	 * 使用「預設」 bucket 取消 Multipart Upload
	 * 
	 * @param s3Key    S3 對象鍵
	 * @param uploadId Multipart Upload ID
	 */
	public void abortMultipartUpload(String s3Key, String uploadId) {
		abortMultipartUploadInternal(s3Key, uploadId, bucketName);
	}

	/**
	 * 使用「指定」 bucket 取消 Multipart Upload
	 * 
	 * @param s3Key      S3 對象鍵
	 * @param uploadId   Multipart Upload ID
	 * @param bucketName 儲存桶
	 */
	public void abortMultipartUpload(String s3Key, String uploadId, String bucketName) {
		abortMultipartUploadInternal(s3Key, uploadId, bucketName);
	}

	/**
	 * 共用的取消 Multipart Upload 邏輯（internal）
	 * 
	 * @param s3Key      S3 對象鍵
	 * @param uploadId   Multipart Upload ID
	 * @param bucketName 儲存桶
	 */
	private void abortMultipartUploadInternal(String s3Key, String uploadId, String bucket) {
		try {
			AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
					.bucket(bucket)
					.key(s3Key)
					.uploadId(uploadId)
					.build();

			s3Client.abortMultipartUpload(abortRequest);

			log.info("已取消 S3 Multipart Upload: uploadId={}, s3Key={}", uploadId, s3Key);

		} catch (S3Exception e) {
			log.error("取消 Multipart Upload 失敗: uploadId={}, s3Key={}", uploadId, s3Key, e);
		}
	}

	// ========================================
	// 內部類：分片資訊
	// ========================================

	/**
	 * 分片資訊（partNumber 和 eTag）
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PartInfo {
		private Integer partNumber;
		private String eTag;
	}

	/**
	 * 查看文件對象 (S3 使用 ListObjectsV2，支援 >1000 分頁)
	 */
	public List<ObjectItem> listObjects(String bucketName, String path) {

		String normalizePath = this.normalizePath(path);

		List<ObjectItem> objectItems = new ArrayList<>();

		String continuationToken = null;

		try {
			do {
				// 1. 建立分頁請求
				ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
						.bucket(bucketName)
						.prefix(normalizePath)
						.maxKeys(1000); // AWS 規範最大 1000

				// 有 token 就放進去 , 這樣它會往下一頁去抓Object
				if (continuationToken != null) {
					builder.continuationToken(continuationToken);
				}

				ListObjectsV2Request request = builder.build();

				// 2. 呼叫 API
				ListObjectsV2Response response = s3Client.listObjectsV2(request);

				// 3. 解析回傳內容
				for (S3Object s3Object : response.contents()) {

					// 排除資料夾自身
					if (s3Object.key().equals(path + "/")) {
						continue;
					}

					ObjectItem item = new ObjectItem();
					item.setObjectName(s3Object.key());
					item.setSize(s3Object.size());
					objectItems.add(item);

				}

				System.out.println("Fetched page, keys count = " + response.contents().size());
				System.out.println("Is truncated: " + response.isTruncated());
				System.out.println("Next token: " + response.nextContinuationToken());

				// 4. 是否還有下一頁？ 如果有就把token 存起來
				continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;

			} while (continuationToken != null); // 繼續下一頁直到取完所有物件

		} catch (S3Exception e) {
			e.printStackTrace();
			return null;
		}

		return objectItems;
	}

	/**
	 * 刪除文件對象
	 * 
	 * @param bucketName
	 * @param s3Key
	 */
	private void deleteByKey(String bucketName, String s3Key) {
		
		String normalizeFilePath = this.normalizeFilePath(s3Key);

		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(normalizeFilePath) // S3 中稱為 Key
					.build();

			s3Client.deleteObject(deleteObjectRequest);
		} catch (S3Exception | IllegalArgumentException e) {
			log.error("刪除文件 {} 失敗: {}", s3Key, e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * 嚴格模式,fileName 「不能」 為null 或者 空字串，否則報錯<br>
	 * 刪除文件對象 (S3 使用 deleteObject)
	 *
	 * @param bucketName
	 * @param fileName   (S3 Key)
	 * @return
	 */
	public void removeFile(String bucketName, String fileName) {
		this.deleteByKey(bucketName, fileName);
	}
	
	/**
	 * 寬容模式,fileName 「可以」 為null 或者 空字串，會忽略此次刪除且不會報錯<br>
	 * 刪除文件對象 (S3 使用 deleteObject)
	 * 
	 * @param bucketName
	 * @param fileName
	 */
	public void removeFileIfPresent(String bucketName, String fileName) {
		// 如果fileName 有值,則繼續刪除操作
		if(StringUtils.isNotBlank(fileName)) {
			this.deleteByKey(bucketName, fileName);
		}
		
		// 如果fileName為null 或 空字串,什麼都不做,也不報錯
		
	}

	/**
	 * 批量刪除文件對象 (循環調用 deleteObject)
	 *
	 * @param bucketName 存儲bucket名稱
	 * @param objects    對象名稱集合 (S3 Keys)
	 */
	public void removeFiles(String bucketName, List<String> objectPaths) {
		if (objectPaths == null || objectPaths.isEmpty()) {
			return;
		}

		// 1. 將文件路徑轉換為 S3 的 ObjectIdentifier 列表
		List<ObjectIdentifier> objectIds = objectPaths.stream()
				// 在此處調用S3 key正規化方法
				.map(this::normalizeFilePath)
				.map(key -> ObjectIdentifier.builder().key(key).build())
				.collect(Collectors.toList());

		try {
			// 2. 構建 DeleteObjects 請求
			DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
					.bucket(bucketName)
					.delete(d -> d.objects(objectIds))
					.build();

			// 3. 發送批量刪除請求 (單次請求可刪除最多 1000 個對象)
			DeleteObjectsResponse response = s3Client.deleteObjects(deleteObjectsRequest);

			// 4. 檢查是否有刪除失敗的對象 (S3 的 DeleteObjects 會回傳失敗清單)
			if (response.hasErrors()) {
				response.errors().forEach(error -> {
					log.error("批量刪除失敗: Key={}, Code={}, Message={}", error.key(), error.code(), error.message());
				});
			}

			log.info("成功批量刪除 {} 個對象。其中 {} 個對象刪除失敗。", response.deleted().size() + response.errors().size(),
					response.errors().size());

		} catch (S3Exception e) {
			log.error("批量刪除文件失敗: {}", e.getMessage(), e);
			throw new RuntimeException("批量刪除文件失敗", e);
		}
	}

	/**
	 * 刪除指定「資料夾」(S3 Prefix) 下的所有對象。
	 * 此方法會複用 listObjects 方法來分頁獲取所有文件 Key，然後批量刪除。
	 *
	 * @param bucketName S3 Bucket 名稱
	 * @param folderPath 要刪除的資料夾路徑 (即 S3 Key 的 Prefix)
	 */
	public void removeFolder(String bucketName, String folderPath) {
		if (folderPath == null || folderPath.trim().isEmpty()) {
			log.error("資料夾路徑無效，操作終止。");
			return;
		}

		log.info("開始刪除資料夾下的所有對象：Bucket={}, Folder={}", bucketName, folderPath);

		// 1. 【複用】調用您現有的 listObjects 方法，它已處理分頁和正規化。
		// 注意：listObjects 返回的是所有 ObjectItem 的 Key。
		List<ObjectItem> itemsToDelete = this.listObjects(bucketName, folderPath);

		if (itemsToDelete == null || itemsToDelete.isEmpty()) {
			log.info("Prefix={} 下沒有找到任何對象需要刪除。", folderPath);
			return;
		}

		// 2. 提取所有需要刪除的 Object Key
		List<String> keysToDelete = itemsToDelete.stream().map(ObjectItem::getObjectName).collect(Collectors.toList());

		// 3. 將 Key 列表分批 (每批最多 1000 個) 並批量刪除。
		// 雖然 listObjects 已經分頁獲取，但我們一次性拿到了所有 Key。
		// 為了避免一次性傳遞一個非常大的 List 導致內存問題或單次 DeleteObjects 請求超限制，
		// 仍然需要將 keysToDelete 分成 <= 1000 個一組的小批次進行批量刪除。

		final int BATCH_SIZE = 1000;
		int totalDeletedCount = 0;

		for (int i = 0; i < keysToDelete.size(); i += BATCH_SIZE) {
			// 取出當前的批次 (Batch)
			List<String> currentBatch = keysToDelete.subList(i, Math.min(i + BATCH_SIZE, keysToDelete.size()));

			log.info("正在處理第 {} 批次，共 {} 個對象。", (i / BATCH_SIZE) + 1, currentBatch.size());

			// 4. 【複用】調用您高效的批量刪除方法。
			// 這裡需要修改 removeFiles，讓它處理批量刪除邏輯，而不是循環單獨調用 removeFile。
			// **假設您已經將 removeFiles 更改為 S3 最佳實踐的批量刪除方法 (使用 DeleteObjects)。**
			this.removeFiles(bucketName, currentBatch);

			totalDeletedCount += currentBatch.size();
		}

		log.info("資料夾刪除操作完成：Prefix={}。總共刪除了 {} 個對象。", folderPath, totalDeletedCount);
	}

	/**
	 * 從「預設」 bucket , 查找檔案並產生時效為一天的預簽名URL
	 */
	public String getFilePresignUrl(String filePath) {
		return buildPresignUrl(filePath, bucketName);
	}

	/**
	 * 從「指定」 bucket , 查找檔案並產生時效為一天的預簽名URL
	 */
	public String getFilePresignUrl(String filePath, String bucketName) {
		return buildPresignUrl(filePath, bucketName);
	}

	/**
	 * 共用方法：建立 Presign URL
	 */
	private String buildPresignUrl(String filePath, String bucket) {

		String normalizeFilePath = this.normalizeFilePath(filePath);

		Duration expiryDuration = Duration.ofDays(1);

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(expiryDuration)
				.getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(normalizeFilePath).build())
				.build();

		PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
		return presigned.url().toString();

	}

	/**
	 * 從 「預設」Bucket 流式下載某個資料夾內所有的檔案(壓縮檔)
	 * 
	 * @param folderName  資料夾
	 * @param renameRules 重命名規則
	 * @return
	 */
	public ResponseEntity<StreamingResponseBody> downloadFolderZipByStream(String folderName,
			Map<String, String> renameRules) {

		return this.downloadFolderZipInternal(folderName, renameRules, bucketName);
	}

	/**
	 * 從 「指定」Bucket 流式下載某個資料夾內所有的檔案(壓縮檔)
	 * 
	 * @param folderName
	 * @param renameRules
	 * @param bucketName
	 * @return
	 */
	public ResponseEntity<StreamingResponseBody> downloadFolderZipByStream(String folderName,
			Map<String, String> renameRules, String bucketName) {

		return this.downloadFolderZipInternal(folderName, renameRules, bucketName);
	}

	/*
	 * ---------------------------------------
	 * 共用核心邏輯（真正執行的地方）
	 * ---------------------------------------
	 */
	private ResponseEntity<StreamingResponseBody> downloadFolderZipInternal(String folderName,
			Map<String, String> renameRules, String bucketName) {

		StreamingResponseBody responseBody = outputStream -> {

			try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {

				List<ObjectItem> listObjects = this.listObjects(bucketName, folderName);

				// 初始化被使用的檔名
				Set<String> usedNames = new HashSet<>();

				for (ObjectItem objectItem : listObjects) {
					String originalName = objectItem.getObjectName();

					// 重命名檔名
					String relativePath = renameRules.getOrDefault(originalName,
							originalName.substring(folderName.length() + 1));

					String uniquePath = this.resolveDuplicateName(relativePath, usedNames);
					usedNames.add(uniquePath);

					ZipEntry zipEntry = this.buildZipEntry(objectItem.getObjectName(), uniquePath, zipOut);
					zipOut.putNextEntry(zipEntry);

					// S3 Streaming 讀取
					try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(
							GetObjectRequest.builder().bucket(bucketName).key(objectItem.getObjectName()).build())) {

						byte[] buffer = new byte[4096];
						int bytesRead;

						while ((bytesRead = in.read(buffer)) != -1) {
							zipOut.write(buffer, 0, bytesRead);
						}

						zipOut.closeEntry();
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=" + folderName + ".zip")
				.body(responseBody);
	}

	/*
	 * ---------------------------------------
	 * 抽出的輔助功能：命名衝突處理
	 * ---------------------------------------
	 */
	private String resolveDuplicateName(String relativePath, Set<String> usedNames) {
		String unique = relativePath;
		int dup = 1;

		while (usedNames.contains(unique)) {
			int dot = relativePath.lastIndexOf('.');
			if (dot > -1) {
				unique = relativePath.substring(0, dot) + "_" + dup++ + relativePath.substring(dot);
			} else {
				unique = relativePath + "_" + dup++;
			}
		}
		return unique;
	}

	/*
	 * ---------------------------------------
	 * 抽出的輔助功能：ZIP Entry 建置與壓縮策略
	 * ---------------------------------------
	 */
	private ZipEntry buildZipEntry(String objectName, String uniquePath, ZipOutputStream zipOut) {
		ZipEntry zipEntry = new ZipEntry(uniquePath);

		// 已壓縮檔案 → 不再壓縮
		if (isAlreadyCompressed(objectName)) {
			zipEntry.setMethod(ZipEntry.DEFLATED);
			zipOut.setLevel(Deflater.NO_COMPRESSION);
		}
		return zipEntry;
	}

	// ----------------------------------------------------
	// 以下辅助方法不涉及 API 调用，逻辑保持不变
	// ----------------------------------------------------

	/**
	 * 提取檔名 及 副檔名
	 * 
	 * @param fileName
	 * @return
	 */
	private FileNameParts extractNameParts(String fileName) {
		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex == -1) {
			return new FileNameParts(fileName, "");
		}
		String baseName = fileName.substring(0, lastDotIndex);
		String extension = fileName.substring(lastDotIndex);
		return new FileNameParts(baseName, extension);
	}

	/**
	 * 範化路徑：
	 * 1. 移除開頭的 "/"（如果有的話）
	 * 2. 確保結尾有 "/"
	 * 
	 * @param path 儲存路徑,不含檔名
	 * @return
	 */
	public String normalizePath(String path) {
		
		if (path == null) {
			throw new IllegalArgumentException("path 不能為 null");
		}

		String p = path.trim();
		if (p.isEmpty()) {
			throw new IllegalArgumentException("path 不能為空字串");
		}

		// 移除所有開頭的 '/'
		int start = 0;
		while (start < p.length() && p.charAt(start) == '/') {
			start++;
		}
		p = p.substring(start);

		// 移除尾部所有的 '/'，最後再補一個 '/' 回去
		int end = p.length() - 1;
		while (end >= 0 && p.charAt(end) == '/') {
			end--;
		}
		p = p.substring(0, end + 1) + "/";

		return p;

	}

	/**
	 * 範化路徑：
	 * 1. 移除開頭的 "/"（如果有的話）
	 * 
	 * @param filePath 檔案S3儲存路徑
	 * @return
	 */
	public String normalizeFilePath(String filePath) {
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("path 不能為NULL 或 空字串");
		}

		// 移除開頭的 "/"
		String normalized = filePath.startsWith("/") ? filePath.substring(1) : filePath;

		return normalized;
	}

	/**
	 * 通常HTML src屬性中會帶有http://domain/...之類的，這邊要排除前墜，用於提取真正S3內檔案的儲存路徑
	 * 
	 * @param bucketName
	 * @param urls
	 * @return
	 */
	public List<String> extractPaths(String bucketName, List<String> urls) {
		List<String> paths = new ArrayList<>();
		Pattern pattern = Pattern.compile("/" + bucketName + "/(.+)");

		for (String url : urls) {
			Matcher matcher = pattern.matcher(url);
			if (matcher.find()) {
				paths.add(matcher.group(1));
			}
		}

		return paths;
	}

	/**
	 * 資料庫中的檔案路徑會加上buckName儲存， 此功能用來抽取S3實際儲存的地
	 *
	 * @param bucketName S3 Bucket
	 * @param path       儲存在資料庫的路徑
	 * @return
	 */
	public String extractS3PathInDbUrl(String bucketName, String path) {
		String minioPath = path.replaceFirst("^/" + bucketName + "/", "");
		return minioPath;
	}

	/**
	 * 
	 * @param bucketName
	 * @param objectPath
	 * @return
	 */
	public String formatDbUrl(String bucketName, String objectPath) {
		// 確保 objectPath 去掉開頭的 "/"（如果有的話）
		String cleanPath = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
		return "/" + bucketName + "/" + cleanPath;
	}

	/**
	 * 獲取檔案後綴名的方法
	 * ... (逻辑不变)
	 */
	public String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex != -1) {
			return "." + fileName.substring(dotIndex + 1);
		}
		return "";
	}

	// 檢查是否為已壓縮檔案 (無須二次壓縮)
	private boolean isAlreadyCompressed(String filename) {
		String[] compressedExtensions = {
				// office 带x的档案
				".docx", ".xlsx", ".pptx",
				// 多媒体
				".mp4", ".mkv", ".mov", ".mp3", ".aac", ".ogg", ".jpg", ".jpeg", ".png", ".gif", ".webp",
				// 文件/存檔
				".pdf", ".zip", ".rar", ".7z", ".gz", ".bz2", ".xz",
				// 二进位
				".exe", ".dll", ".deb", ".rpm", ".apk", ".ipa" };

		return Arrays.stream(compressedExtensions).anyMatch(filename::endsWith);
	}
}
