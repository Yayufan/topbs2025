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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.InvitedSpeakerConvert;
import tw.com.topbs.mapper.InvitedSpeakerMapper;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddInvitedSpeakerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutInvitedSpeakerDTO;
import tw.com.topbs.pojo.entity.InvitedSpeaker;
import tw.com.topbs.service.InvitedSpeakerService;
import tw.com.topbs.utils.MinioUtil;

/**
 * <p>
 * 受邀請的講者，可能是講者，可能是座長 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-04-23
 */
@Service
@RequiredArgsConstructor
public class InvitedSpeakerServiceImpl extends ServiceImpl<InvitedSpeakerMapper, InvitedSpeaker>
		implements InvitedSpeakerService {

	private final InvitedSpeakerConvert invitedSpeakerConvert;
	private final MinioUtil minioUtil;

	@Value("${minio.bucketName}")
	private String minioBucketName;

	@Override
	public InvitedSpeaker getInvitedSpeaker(Long id) {
		InvitedSpeaker invitedSpeaker = baseMapper.selectById(id);
		return invitedSpeaker;
	}

	@Override
	public List<InvitedSpeaker> getAllInvitedSpeaker() {
		List<InvitedSpeaker> invitedSpeakerList = baseMapper.selectList(null);
		return invitedSpeakerList;
	}

	@Override
	public IPage<InvitedSpeaker> getInvitedSpeakerPage(Page<InvitedSpeaker> page) {
		Page<InvitedSpeaker> invitedSpeakerPage = baseMapper.selectPage(page, null);
		return invitedSpeakerPage;
	}

	@Override
	public IPage<InvitedSpeaker> getInvitedSpeakerPage(Page<InvitedSpeaker> page, String queryText) {

		LambdaQueryWrapper<InvitedSpeaker> invitedSpeakerWrapper = new LambdaQueryWrapper<>();
		invitedSpeakerWrapper.like(StringUtils.isNoneBlank(queryText), InvitedSpeaker::getName, queryText);

		Page<InvitedSpeaker> invitedSpeakerPage = baseMapper.selectPage(page, invitedSpeakerWrapper);
		return invitedSpeakerPage;
	}

	@Override
	public void addInvitedSpeaker(MultipartFile file, AddInvitedSpeakerDTO addInvitedSpeakerDTO) {

		//資料轉換成實體類
		InvitedSpeaker invitedSpeaker = invitedSpeakerConvert.addDTOToEntity(addInvitedSpeakerDTO);

		// 判斷如有檔案
		if (file != null && !file.isEmpty()) {
			System.out.println("新增，有檔案");

			// 處理檔名和擴展名
			String originalFilename = file.getOriginalFilename();
			//String fileExtension = minioUtil.getFileExtension(originalFilename);

			// 投稿摘要基本檔案路徑
			String path = "invited-speaker/";

			// 上傳檔案至Minio,
			// 獲取回傳的檔案URL路徑,加上minioBucketName 
			String uploadUrl = minioUtil.upload(minioBucketName, path, originalFilename, file);
			uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

			// 設定檔案路徑
			invitedSpeaker.setPhotoUrl(uploadUrl);

		}

		// 最後都insert 進資料庫
		baseMapper.insert(invitedSpeaker);

	}

	@Override
	public void updateInvitedSpeaker(MultipartFile file, @Valid PutInvitedSpeakerDTO putInvitedSpeakerDTO) {
		InvitedSpeaker invitedSpeaker = invitedSpeakerConvert.putDTOToEntity(putInvitedSpeakerDTO);

		// 判斷如有檔案
		if (file != null && !file.isEmpty()) {
			System.out.println("更新，有檔案");

			//先找到之前的儲存的檔案路徑
			InvitedSpeaker currentInvitedSpeaker = baseMapper.selectById(invitedSpeaker);
			String photoUrl = currentInvitedSpeaker.getPhotoUrl();

			// 如果確定之前有舊檔案路徑，且字串不為空
			if(photoUrl != null && StringUtils.isNotEmpty(photoUrl)) {
				//去掉/minio/這個前墜，才是真正minio儲存的位置
				String objectPath = minioUtil.extractPath(minioBucketName, photoUrl);

				//移除檔案
				minioUtil.removeObject(minioBucketName, objectPath);
			}
			

			//開始新增檔案， 處理檔名和擴展名
			String originalFilename = file.getOriginalFilename();
			//String fileExtension = minioUtil.getFileExtension(originalFilename);

			// 投稿摘要基本檔案路徑
			String path = "invited-speaker/";

			// 上傳檔案至Minio,
			// 獲取回傳的檔案URL路徑,加上minioBucketName 
			String uploadUrl = minioUtil.upload(minioBucketName, path, originalFilename, file);
			uploadUrl = "/" + minioBucketName + "/" + uploadUrl;

			// 設定檔案路徑
			invitedSpeaker.setPhotoUrl(uploadUrl);

		}

		baseMapper.updateById(invitedSpeaker);
	}

	@Override
	public void deleteInvitedSpeaker(Long id) {

		//先找到之前的儲存的檔案路徑
		InvitedSpeaker currentInvitedSpeaker = baseMapper.selectById(id);
		String photoUrl = currentInvitedSpeaker.getPhotoUrl();

		//去掉/minio/這個前墜，才是真正minio儲存的位置
		String objectPath = minioUtil.extractPath(minioBucketName, photoUrl);

		//移除檔案
		minioUtil.removeObject(minioBucketName, objectPath);

		// 移除資料庫資料
		baseMapper.deleteById(id);
	}

	@Override
	public void deleteInvitedSpeakerList(List<Long> ids) {
		for (Long id : ids) {
			this.deleteInvitedSpeaker(id);
		}

	}

}
