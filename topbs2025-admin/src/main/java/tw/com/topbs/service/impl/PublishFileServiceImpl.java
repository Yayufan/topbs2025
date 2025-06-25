package tw.com.topbs.service.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.PublishFileConvert;
import tw.com.topbs.mapper.PublishFileMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddPublishFileDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPublishFileDTO;
import tw.com.topbs.pojo.entity.PublishFile;
import tw.com.topbs.service.PublishFileService;
import tw.com.topbs.utils.MinioUtil;

/**
 * <p>
 * 發佈檔案表 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-02-05
 */
@RequiredArgsConstructor
@Service
public class PublishFileServiceImpl extends ServiceImpl<PublishFileMapper, PublishFile> implements PublishFileService {

	private final PublishFileConvert publishFileConvert;
	private final String BASE_PATH = "publish-file/";

	@Value("${minio.bucketName}")
	private String minioBucketName;

	private final MinioUtil minioUtil;

	@Override
	public List<PublishFile> getAllFileByGroupAndType(String group, String type) {
		LambdaQueryWrapper<PublishFile> fileQueryWrapper = new LambdaQueryWrapper<>();
		fileQueryWrapper.eq(PublishFile::getGroupType, group)
				.eq(StringUtils.isNoneBlank(type), PublishFile::getType, type)
				.orderByAsc(PublishFile::getSort)
				.orderByDesc(PublishFile::getPublishFileId);

		List<PublishFile> fileList = baseMapper.selectList(fileQueryWrapper);

		return fileList;
	}

	@Override
	public IPage<PublishFile> getAllFileByGroup(String group, Page<PublishFile> pageInfo) {
		// 查詢群組、分頁，並倒序排列
		LambdaQueryWrapper<PublishFile> fileQueryWrapper = new LambdaQueryWrapper<>();
		fileQueryWrapper.eq(PublishFile::getGroupType, group)
				.orderByAsc(PublishFile::getType)
				.orderByAsc(PublishFile::getSort)
				.orderByDesc(PublishFile::getPublishFileId);
		Page<PublishFile> fileList = baseMapper.selectPage(pageInfo, fileQueryWrapper);
		return fileList;
	}

	@Override
	public void addPublishFile(MultipartFile file, MultipartFile imgFile, AddPublishFileDTO addPublishFileDTO) {
		PublishFile fileEntity = publishFileConvert.addDTOToEntity(addPublishFileDTO);

		// 文件檔案存在，處理檔案
		if (file != null) {

			// 上傳檔案
			String url = minioUtil.upload(minioBucketName, BASE_PATH, addPublishFileDTO.getGroupType() + "/", file);
			// 將bucketName 組裝進url
			String formatDbUrl = minioUtil.formatDbUrl(minioBucketName, url);
			// minio完整路徑放路對象中
			fileEntity.setPath(formatDbUrl);

		}

		// 縮圖檔案存在，處理檔案
		if (imgFile != null) {

			// 上傳檔案
			String url = minioUtil.upload(minioBucketName, BASE_PATH, addPublishFileDTO.getGroupType() + "/", imgFile);
			// 將bucketName 組裝進url
			String formatDbUrl = minioUtil.formatDbUrl(minioBucketName, url);
			// minio完整路徑放入縮圖中
			fileEntity.setCoverThumbnailUrl(formatDbUrl);

		}

		// 放入資料庫
		baseMapper.insert(fileEntity);

		System.out.println("上傳完成");

	}

	@Override
	public void putPublishFile(MultipartFile file, MultipartFile imgFile, PutPublishFileDTO putPublishFileDTO) {

		PublishFile fileEntity = publishFileConvert.putDTOToEntity(putPublishFileDTO);
		PublishFile oldPublishFile = this.getById(putPublishFileDTO.getPublishFileId());
		// 文件檔案存在，處理檔案
		if (file != null) {

			// 先刪除舊檔案
			String oldFilePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, oldPublishFile.getPath());
			minioUtil.removeObject(minioBucketName, oldFilePathInMinio);

			// 上傳新檔案
			String url = minioUtil.upload(minioBucketName, BASE_PATH, putPublishFileDTO.getGroupType() + "/", file);
			String formatDbUrl = minioUtil.formatDbUrl(minioBucketName, url);
			fileEntity.setPath(formatDbUrl);

		}

		// 縮圖檔案存在，處理檔案
		if (imgFile != null) {

			// 先刪除舊檔案
			String oldFilePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName,
					oldPublishFile.getCoverThumbnailUrl());
			minioUtil.removeObject(minioBucketName, oldFilePathInMinio);

			// 上傳新檔案
			String url = minioUtil.upload(minioBucketName, BASE_PATH, putPublishFileDTO.getGroupType() + "/", imgFile);
			String formatDbUrl = minioUtil.formatDbUrl(minioBucketName, url);
			fileEntity.setCoverThumbnailUrl(formatDbUrl);

		}

		// 更新資料庫
		baseMapper.updateById(fileEntity);

	}

	@Override
	public void deletePublishFile(Long fileId) {
		PublishFile fileEntity = baseMapper.selectById(fileId);

		// 清除檔案
		String filePath = fileEntity.getPath();
		// 因為縮圖圖檔URL有包含 bucketName, 這邊先進行提取
		String filePathInMinio = minioUtil.extractFilePathInMinio(minioBucketName, filePath);
		// 透過Minio進行刪除
		minioUtil.removeObject(minioBucketName, filePathInMinio);

		// 如果此紀錄有縮圖檔案,也要一起刪掉
		if (fileEntity.getCoverThumbnailUrl() != null) {
			String filePathInMinio2 = minioUtil.extractFilePathInMinio(minioBucketName,
					fileEntity.getCoverThumbnailUrl());
			// 透過Minio進行刪除
			minioUtil.removeObject(minioBucketName, filePathInMinio2);
		}

		// 資料庫資料刪除
		baseMapper.deleteById(fileId);

	}

	@Override
	public void deletePublishFile(List<Long> fileIdList) {
		// 遍歷循環刪除
		for (Long fileId : fileIdList) {
			// 去執行單個刪除
			this.deletePublishFile(fileId);

		}

	}

}
