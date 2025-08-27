package tw.com.topbs.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.pojo.sys.ObjectItem;

/**
 * @description： minio工具类 @version：1.0
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioUtil {

	// MinioClient对象，用于与MinIO服务进行交互
	private final MinioClient minioClient;

	@Qualifier("taskExecutor") // 使用您配置的線程池
	private final Executor taskExecutor;

	// 預設存储桶名称
	@Value("${minio.bucketName}")
	private String bucketName;

	/**
	 * description: 判断bucket是否存在，不存在则创建
	 *
	 * @return: void
	 */
	public void existBucket(String name) {

		// 訪問策略 JSON, 這個策略
		/*
		 * Version務必為2012-10-17 Sid : 為PublicRead 公共讀取, 上傳的文件可被任何人讀取 Effect: 規則的作用，這裡是
		 * "Allow"，表示允許訪問。 Principal: 訪問的主體，這裡是 {"AWS":"*"}，表示允許任何 AWS 帳戶訪問。 Action:
		 * 允許執行的動作，這裡是 ["s3:GetObject"]，表示允許從存儲桶中獲取對象。 Resource: 訪問的資源，這裡是
		 * "arn:aws:s3::: + name + "/*"，表示存儲桶下的所有對象。
		 * 
		 */

		try {

			// 检查存储桶是否存在
			boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
			if (!exists) {
				// 如果不存在，则创建存储桶
				minioClient.makeBucket(MakeBucketArgs.builder().bucket(name).build());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除存储bucket
	 *
	 * @param bucketName 存储bucket名称
	 * @return Boolean
	 */
	public Boolean removeBucket(String bucketName) {
		try {
			// 删除存储桶(空桶)
			minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * 
	 * 建立空目錄 這邊要注意的事情是,路徑開頭不要有/號,且路徑最後要有/號,例如: xxx/yyy/
	 * 
	 */

	public String createFolder(String folderPath) {

		try {
			minioClient.putObject(PutObjectArgs.builder()
					.bucket(bucketName)
					.object(folderPath)
					.stream(new ByteArrayInputStream(new byte[] {}), 0, -1)
					.build());
		} catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
				| InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
				| IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "success";
	}

	/**
	 * 獲取 minio 物件的位元組陣列，適用於小檔案20MB以內
	 *
	 * @param filePath 帶有minio bucketName的 Path
	 * @return 檔案的位元組陣列
	 */
	public byte[] getFileBytes(String filePath) {
		String filePathInMinio = this.extractFilePathInMinio(bucketName, filePath);

		try (InputStream inputStream = minioClient
				.getObject(GetObjectArgs.builder().bucket(bucketName).object(filePathInMinio).build())) {

			return inputStream.readAllBytes();

		} catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
				| InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
				| IllegalArgumentException | IOException e) {
			e.printStackTrace();
			log.error(e.getMessage());
			return null;
		}
	}

	/**
	 * 獲取檔案大小
	 * 
	 * @param filePath
	 * @return
	 */
	public long getFileSize(String filePath) {
		// 初始化檔案大小，固定為0
		long fileSize = 0L;

		// 如果沒傳則直接返回0
		if (filePath == null) {
			return fileSize;
		}

		try {

			// 提取Minio內真正的路徑
			String filePathInMinio = this.extractFilePathInMinio(bucketName, filePath);

			// 獲取物件元數據
			StatObjectResponse stat = minioClient
					.statObject(StatObjectArgs.builder().bucket(bucketName).object(filePathInMinio).build());

			fileSize = stat.size();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

		return fileSize;

	}

	/**
	 * 傳一個 pathList，計算所有路徑檔案的Size
	 * 
	 * @param filePaths
	 * @return
	 */
	public long calculateTotalSize(List<String> filePaths) {
		long totalSize = 0L;

		if (filePaths == null || filePaths.isEmpty()) {
			return totalSize;
		}

		for (String filePath : filePaths) {
			try {

				// 提取Minio內真正的路徑
				String filePathInMinio = this.extractFilePathInMinio(bucketName, filePath);

				// 獲取物件元數據
				StatObjectResponse stat = minioClient
						.statObject(StatObjectArgs.builder().bucket(bucketName).object(filePathInMinio).build());

				totalSize += stat.size();
			} catch (Exception e) {
				e.printStackTrace();
				log.error(e.getMessage());
			}
		}

		return totalSize;
	}

	/**
	 * 上傳單文件,重定義檔名
	 * 
	 * @param bucketName
	 * @param path          子路徑（開頭不該有 /，結尾必須有 /，例如 "groupType/"）
	 * @param multipartFile
	 * @return
	 */
	public String upload(String bucketName, String path, String fileName, MultipartFile multipartFile) {

		// 1. 檢查並規範化 path
		String normalizedPath = normalizePath(path);

		// 獲取文件的擴展名
		String extension = "";
		int lastDotIndex = fileName.lastIndexOf(".");

		if (lastDotIndex != -1) {
			extension = fileName.substring(lastDotIndex);
			fileName = fileName.substring(0, lastDotIndex); // 移除擴展名部分
		}

		// 生成新的文件名（在擴展名前添加时间戳）
		String fullFileName = normalizedPath + fileName + "_" + System.currentTimeMillis() + extension;

		InputStream in = null;
		try {
			in = multipartFile.getInputStream();
			// 上传文件到MinIO服务
			minioClient.putObject(PutObjectArgs.builder()
					// 選定bucket
					.bucket(bucketName)
					// 儲存的物件(檔案)的名稱
					.object(fullFileName)
					// 將檔案輸入並上傳
					// in 是通過MultipartFile對象獲取的文件的輸入流。
					// 第二個參數 file.getSize() 表示文件的大小。
					// 第三個參數 -1 表示不限制上傳文件的大小。
					.stream(in, multipartFile.getSize(), -1)
					// 這一行設置了上傳對象的Content-Type，這是指定上傳對象的MIME類型。
					// file.getContentType() 返回的是通過MultipartFile對象獲取的文件的Content-Type。
					.contentType(multipartFile.getContentType())
					.build());

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return fullFileName;
	}

	/**
	 * description: 上传(多)文件
	 * 
	 * @param bucketName    桶名稱
	 * @param path          子路徑（開頭不該有 /，結尾必須有 /，例如 "groupType/"）
	 * @param multipartFile 檔案數組
	 * @return
	 */
	public List<String> upload(String bucketName, String path, MultipartFile[] multipartFile) {
		List<String> names = new ArrayList<>(multipartFile.length);
		for (MultipartFile file : multipartFile) {

			// 1. 檢查並規範化 path
			String normalizedPath = normalizePath(path);

			String fileName = file.getOriginalFilename();

			// 獲取文件的擴展名
			String extension = "";
			int lastDotIndex = fileName.lastIndexOf(".");

			if (lastDotIndex != -1) {
				extension = fileName.substring(lastDotIndex);
				fileName = fileName.substring(0, lastDotIndex); // 移除擴展名部分
			}

			// 生成新的文件名（在擴展名前添加时间戳）
			String fullFileName = normalizedPath + fileName + "_" + System.currentTimeMillis() + extension;

			InputStream in = null;
			try {
				in = file.getInputStream();
				// 上传文件到MinIO服务
				minioClient.putObject(PutObjectArgs.builder()
						// 選定bucket
						.bucket(bucketName)
						// 儲存的物件(檔案)的名稱
						.object(fullFileName)
						// 將檔案輸入並上傳
						// in 是通過MultipartFile對象獲取的文件的輸入流。
						// 第二個參數 file.getSize() 表示文件的大小。
						// 第三個參數 -1 表示不限制上傳文件的大小。
						.stream(in, file.getSize(), -1)
						// 這一行設置了上傳對象的Content-Type，這是指定上傳對象的MIME類型。
						// file.getContentType() 返回的是通過MultipartFile對象獲取的文件的Content-Type。
						.contentType(file.getContentType())
						.build());

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			names.add(fullFileName);
		}
		return names;
	}

	/**
	 * 規範化路徑：
	 * 1. 移除開頭的 "/"（如果有的話）
	 * 2. 確保結尾有 "/"
	 * 
	 * @throws IllegalArgumentException 如果 path 為 null 或空字串
	 */
	private String normalizePath(String path) {
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("path 不能為空");
		}

		// 移除開頭的 "/"
		String normalized = path.startsWith("/") ? path.substring(1) : path;

		// 確保結尾有 "/"
		if (!normalized.endsWith("/")) {
			normalized += "/";
		}

		return normalized;
	}

	/**
	 * description: 下载文件
	 *
	 * @param fileName,因為Bucket是唯一,如果需要下載Bucket中目錄裡的檔案, fileName 必須為 目錄名/檔案名.後墜,
	 *                                                  一定要有後墜!!! Minio
	 *                                                  API目前暫時不能直接下載一個資料夾
	 * 
	 * @return: org.springframework.http.ResponseEntity<byte [ ]>
	 */
	public ResponseEntity<byte[]> download(String fileName) {
		ResponseEntity<byte[]> responseEntity = null;
		InputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			// 从MinIO服务下载文件,getObject是返回一個inputStream
			in = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());

			// 創建一個 ByteArrayOutputStream 對象，用於將下載的文件內容寫入內存中。
			out = new ByteArrayOutputStream();

			// 使用 Apache Commons IO 中的 IOUtils 工具類，從輸入流 in 中讀取數據並將其寫入輸出流 out 中。
			IOUtils.copy(in, out);
			// 封装返回值
			// 從 ByteArrayOutputStream 中獲取所有寫入的字節數據並將其轉換為字節數組，其實就是轉回基本類型
			byte[] bytes = out.toByteArray();

			// 因為返回的是整個響應報文,所以需要內容、請求頭、響應狀態
			HttpHeaders headers = new HttpHeaders();
			try {

				/*
				 * 
				 * Content-Disposition 標頭的一些常見用法包括：
				 * 
				 * inline：將檔案直接顯示在瀏覽器中，通常用於顯示圖片、PDF等檔案。 attachment：提示瀏覽器下載檔案，而不是直接顯示，通常用於下載附件。
				 * 指定檔案名稱：可以通過 filename 參數來指定下載的檔案名稱，這樣可以讓下載的檔案擁有一個明確的名稱，而不是根據URL或其他方式猜測。
				 * 總的來說，Content-Disposition 標頭提供了對於HTTP回應中附加檔案的更精確控制，能夠影響到瀏覽器如何處理這些檔案。
				 * 
				 */

				// 將文件名添加到 HTTP 響應的 Content-Disposition 標頭中，
				// 以便客戶端將其作為附件下載。文件名使用 UTF-8 編碼進行 URL 編碼以處理可能的特殊字符。
				headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			// 設置 HTTP 響應的 Content-Length 標頭，指定返回內容的字節長度。
			headers.setContentLength(bytes.length);

			// 重要!! 設置 HTTP 響應的 Content-Type 標頭為應用程序八位元組流，表示返回的是二進制數據。
			headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			// 設置 HTTP 響應的訪問控制暴露標頭，允許客戶端訪問所有標頭信息。
			headers.setAccessControlExposeHeaders(Arrays.asList("*"));
			// 創建一個 ResponseEntity 對象，封裝字節數組、標頭信息和 HTTP 狀態碼。這將作為下載請求的響應。
			responseEntity = new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// 如果輸入流不為空，則關閉輸入流。在關閉過程中可能會發生 IO 異常，進行捕獲和打印。
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return responseEntity;
	}

	/**
	 * 查看文件对象
	 *
	 * @param bucketName 存储bucket名称
	 * @return 存储bucket内文件对象信息
	 */
	public List<ObjectItem> listObjects(String bucketName, String path) {
		Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
				// 查詢某個路徑(資料夾)下的物件(檔案),這邊要注意的是,前面不需加/ , 但是路徑後面需要
				.prefix(path + "/")
				// 遞迴的recursive , true為進入該路徑繼續找尋物件
				.recursive(true)
				.bucket(bucketName)
				.build());
		List<ObjectItem> objectItems = new ArrayList<>();
		try {
			// 遍历结果集，获取文件对象信息
			for (Result<Item> result : results) {
				Item item = result.get();
				// Skip directories
				//                if (item.isDir()) continue;

				ObjectItem objectItem = new ObjectItem();
				objectItem.setObjectName(item.objectName());
				objectItem.setSize(item.size());
				objectItems.add(objectItem);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return objectItems;
	}

	/**
	 * fileName 如果有包含bucketName的路徑要記得移除
	 * 
	 * @param bucketName
	 * @param fileName
	 * @return
	 */
	public String removeObject(String bucketName, String fileName) {
		// 删除文件
		try {
			minioClient.removeObject(RemoveObjectArgs.builder()
					.bucket(bucketName) // 替换为你实际的存储桶名称
					.object(fileName)
					.build());
		} catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
				| InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
				| IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "success";
	}

	/**
	 * 批量删除文件对象
	 *
	 * @param bucketName 存储bucket名称
	 * @param objects    对象名称集合
	 */

	public String removeObjects(String bucketName, List<String> objects) {

		objects.forEach(fileName -> {
			System.out.println(fileName);
			// 删除文件
			try {
				minioClient.removeObject(RemoveObjectArgs.builder()
						.bucket(bucketName) // 替换为你实际的存储桶名称
						.object(fileName)
						.build());
			} catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
					| InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
					| IllegalArgumentException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});

		return "success";
	}

	//	public Iterable<Result<DeleteError>> removeObjects(String bucketName, List<String> objects) {
	//		// 构建DeleteObject对象集合
	//		List<DeleteObject> dos = objects.stream().map(e -> new DeleteObject(e)).collect(Collectors.toList());
	//		System.out.println("這是DeleteObject對象" + dos);
	//		// 批量删除文件对象
	//		Iterable<Result<DeleteError>> results = minioClient
	//				.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(dos).build());
	//		return results;
	//	}

	/*
	 * 創建文件Url
	 * 
	 * 
	 */
	public String getFileUrl(String fileName) throws InvalidKeyException, ErrorResponseException,
			InsufficientDataException, InternalException, InvalidResponseException, NoSuchAlgorithmException,
			XmlParserException, ServerException, IllegalArgumentException, IOException {
		String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
				.method(Method.GET)
				.bucket(bucketName)
				.object(fileName)
				.expiry(1, TimeUnit.DAYS)
				.build());
		System.out.println(url);

		return url;
	}

	/**
	 * 通常HTML src屬性中會帶有http://domain/...之類的，這邊要排除前墜，用於提取真正Minio內檔案的儲存路徑
	 * <p>
	 * 正則表達式會找到 "/my-bucket/images/cat.jpg" 這個匹配
	 * </p>
	 * <p>
	 * matcher.group(1) 會提取捕獲組中的內容，也就是 "images/cat.jpg"
	 * </p>
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
	 * 提取單一 URL 的物件路徑（去除 bucket 名稱）。
	 * 對於/topbs2025/invited-speaker圖片1_1745399281172.png可以找出objectPath
	 * 或者https://domain/buckName/...之類的URL也可以提取
	 * 
	 * @param bucketName MinIO 的 bucket 名稱，例如 "topbs2025"
	 * @param url        原始 URL，例如 "/topbs2025/invited-speaker圖片1_1745399281172.png"
	 * @return 提取後的物件 key，例如 "invited-speaker圖片1_1745399281172.png"
	 */
	public String extractPath(String bucketName, String url) {
		Pattern pattern = Pattern.compile("/" + bucketName + "/(.+)");
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("URL 不包含指定的 bucket 名稱: " + bucketName);
	}

	/**
	 * 資料庫中的檔案路徑會加上buckName儲存， 此功能用來抽取minio實際儲存的地址
	 * 
	 * @param bucketName Minio Bucket
	 * @param path       儲存在資料庫的路徑
	 * @return
	 */
	public String extractFilePathInMinio(String bucketName, String path) {
		String minioPath = path.replaceFirst("^/" + bucketName + "/", "");
		return minioPath;
	}

	public String formatDbUrl(String bucketName, String objectPath) {
		// 確保 objectPath 去掉開頭的 "/"（如果有的話）
		String cleanPath = objectPath.startsWith("/") ? objectPath.substring(1) : objectPath;
		return "/" + bucketName + "/" + cleanPath;
	}

	/**
	 * 獲取檔案後綴名的方法
	 * 
	 * @param fileName
	 * @return
	 */
	public String getFileExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf(".");
		if (dotIndex != -1) {
			return fileName.substring(dotIndex + 1);
		}
		return "";
	}

	// 流式下載資料夾打包並壓縮的ZIP檔案，需要傳送minio的資料夾
	public ResponseEntity<StreamingResponseBody> downloadFolderZipByStream(String folderName) {

		StreamingResponseBody responseBody = outputStream -> {
			// 在這裡生成數據並寫入 outputStream

			// 創建zip的OutputStream
			try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {

				// 從minio獲取資料夾內的檔案列表
				List<ObjectItem> listObjects = this.listObjects(bucketName, folderName);

				for (ObjectItem objectItem : listObjects) {
					System.out.println("物件名為: " + objectItem.getObjectName());
					System.out.println("物件Size為: " + objectItem.getSize() + " byte");

					// Preserve original path structure within ZIP ， 跟objectName 只差在 有沒有paper/ 這層
					String relativePath = objectItem.getObjectName().substring(folderName.length() + 1);

					// Create ZIP entry
					ZipEntry zipEntry = new ZipEntry(relativePath);

					// 跳過壓縮已壓縮的檔案
					if (isAlreadyCompressed(objectItem.getObjectName())) {

						System.out.println("此為被壓縮的檔案，跳過當前檔案");

						// DEFLATED + NO_COMPRESSION
						// 行為類似ZipEntry.STORED（不壓縮），但 ZIP 規範不要求 CRC-32(會造成阻塞)。
						// 完全支持流式傳輸，無需預讀檔案。
						zipEntry.setMethod(ZipEntry.DEFLATED);
						zipOut.setLevel(Deflater.NO_COMPRESSION); // 關鍵設定！

					}

					zipOut.putNextEntry(zipEntry);

					try {

						// 獲取物件的inputStream流
						InputStream in = minioClient.getObject(
								GetObjectArgs.builder().bucket(bucketName).object(objectItem.getObjectName()).build());

						byte[] buffer = new byte[4096];
						int bytesRead;

						while ((bytesRead = in.read(buffer)) != -1) {
							zipOut.write(buffer, 0, bytesRead);
							zipOut.flush();
						}
						;

						zipOut.closeEntry();

					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}

				}

			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}

		};

		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=" + folderName + ".zip")
				.body(responseBody);

	}

	// 檢查是否為已壓縮檔案（無需二次壓縮）
	private boolean isAlreadyCompressed(String filename) {
		String[] compressedExtensions = {
				// office 帶x的檔案
				".docx", ".xlsx", ".pptx",
				// 多媒體
				".mp4", ".mkv", ".mov", ".mp3", ".aac", ".ogg", ".jpg", ".jpeg", ".png", ".gif", ".webp",
				// 文件/存檔
				".pdf", ".zip", ".rar", ".7z", ".gz", ".bz2", ".xz",
				// 二進位
				".exe", ".dll", ".deb", ".rpm", ".apk", ".ipa" };

		return Arrays.stream(compressedExtensions).anyMatch(filename::endsWith);
	}
}
