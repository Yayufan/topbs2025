package tw.com.topbs.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.SettingConvert;
import tw.com.topbs.enums.RegistrationPhaseEnum;
import tw.com.topbs.exception.SettingException;
import tw.com.topbs.mapper.SettingMapper;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutSettingDTO;
import tw.com.topbs.pojo.entity.Setting;
import tw.com.topbs.service.SettingService;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl extends ServiceImpl<SettingMapper, Setting> implements SettingService {

	private final SettingConvert settingConvert;

	// 定義單一系統設定紀錄的 ID，由於資料庫中只有一筆，其 ID 通常為 1
	private static final Long SINGLE_SETTING_RECORD_ID = 1L;

	@Override
	public Setting getSetting() {
		// 透過 Mybatis-Plus 的 baseMapper 根據預設的單一紀錄 ID 來查詢
		return baseMapper.selectById(SINGLE_SETTING_RECORD_ID);
	}

	@Override
	public void updateSetting(PutSettingDTO putSettingDTO) {
		Setting setting = this.getSetting();
		// 檢查系統設定紀錄是否存在，如果不存在則拋出異常，因為更新需要有前置紀錄。
		if (setting == null) {
			throw new SettingException("系統設定紀錄不存在，無法進行更新操作。");
		}

		// 使用轉換器將 DTO 中的屬性值更新到現有的 Setting 實體中。
		// 這樣可以保持服務層的邏輯清晰，將物件映射的細節交由轉換器處理。
		Setting newSetting = settingConvert.putDTOToEntity(putSettingDTO);

		// 透過 Mybatis-Plus 更新資料庫中的紀錄。
		baseMapper.updateById(newSetting);
	}

	@Override
	public Boolean isAbstractSubmissionOpen() {
		Setting setting = this.getSetting();
		// 檢查設定是否存在，以及摘要投稿的開始和結束時間是否都已設定。
		if (setting == null || setting.getAbstractSubmissionStartTime() == null
				|| setting.getAbstractSubmissionEndTime() == null) {
			throw new SettingException("投稿摘要設置不完整：請檢查開放投稿時間和截止時間是否已配置。");
		}
		LocalDateTime now = LocalDateTime.now(); // 取得當前時間
		// 調用輔助方法判斷當前時間是否在指定區間內。
		return isBetweenInclusiveStartExclusiveEnd(now, setting.getAbstractSubmissionStartTime(),
				setting.getAbstractSubmissionEndTime());
	}

	/**
	 * 根據 給予的時間 判斷處於哪個早鳥階段
	 * 
	 * @param time
	 * @return
	 */
	private RegistrationPhaseEnum resolvePhase(LocalDateTime time) {
		Setting setting = this.getSetting();
		if (setting == null) {
			return RegistrationPhaseEnum.REGULAR;
		}

		if (isInPhase(time, setting.getEarlyBirdDiscountPhaseOneDeadline())) {
			return RegistrationPhaseEnum.PHASE_ONE;
		}

		if (isInPhase(time, setting.getEarlyBirdDiscountPhaseTwoDeadline())) {
			return RegistrationPhaseEnum.PHASE_TWO;
		}

		if (isInPhase(time, setting.getEarlyBirdDiscountPhaseThreeDeadline())) {
			return RegistrationPhaseEnum.PHASE_THREE;
		}

		if (isInPhase(time, setting.getLastRegistrationTime())) {
			return RegistrationPhaseEnum.REGULAR;
		}

		return RegistrationPhaseEnum.ON_SITE;
	}

	/**
	 * 判斷某個時間 time 是否落在「某個截止時間」之前（含等於）。
	 * 
	 * @param time
	 * @param deadline
	 * @return
	 */
	private boolean isInPhase(LocalDateTime time, LocalDateTime deadline) {
		return deadline != null && (time.isBefore(deadline) || time.isEqual(deadline));
	}

	@Override
	public RegistrationPhaseEnum getRegistrationPhaseEnum() {
		return resolvePhase(LocalDateTime.now());
	}

	@Override
	public RegistrationPhaseEnum getRegistrationPhaseEnum(LocalDateTime targetDateTime) {
		return resolvePhase(targetDateTime);
	}

	@Override
	public Boolean canPlaceOrder() {
		Setting setting = this.getSetting();
		// 檢查設定是否存在，以及最後下訂單時間是否已設定。
		if (setting == null || setting.getLastOrderTime() == null) {
			throw new SettingException("訂單設置不完整：請檢查最後下訂單時間是否已配置。");
		}
		LocalDateTime now = LocalDateTime.now();
		return now.isBefore(setting.getLastOrderTime()) || now.isEqual(setting.getLastOrderTime());
	}

	@Override
	public Boolean isRegistrationOpen() {
		Setting setting = this.getSetting();
		// 檢查設定是否存在，以及最後註冊時間是否已設定。
		if (setting == null || setting.getLastRegistrationTime() == null) {
			throw new SettingException("註冊設置不完整：請檢查最後註冊時間是否已配置。");
		}
		LocalDateTime now = LocalDateTime.now();
		return now.isBefore(setting.getLastRegistrationTime()) || now.isEqual(setting.getLastRegistrationTime());
	}

	@Override
	public Boolean isGroupRegistrationOpen() {
		Setting setting = this.getSetting();
		// 檢查設定是否存在，以及最後 團體報名 註冊時間是否已設定。
		if (setting == null || setting.getLastRegistrationTime() == null) {
			throw new SettingException("團體報名 註冊設置不完整：請檢查最後團體報名註冊時間是否已配置。");
		}
		LocalDateTime now = LocalDateTime.now();
		return now.isBefore(setting.getLastGroupRegistrationTime())
				|| now.isEqual(setting.getLastGroupRegistrationTime());

	}

	@Override
	public Boolean isSlideUploadOpen() {
		Setting setting = this.getSetting();
		// 檢查設定是否存在，以及 Slide 上傳的開始和結束時間是否都已設定。
		if (setting == null || setting.getSlideUploadStartTime() == null || setting.getSlideUploadEndTime() == null) {
			throw new SettingException("Slide上傳設置不完整：請檢查開放上傳時間和截止時間是否已配置。");
		}
		LocalDateTime now = LocalDateTime.now();
		return isBetweenInclusiveStartExclusiveEnd(now, setting.getSlideUploadStartTime(),
				setting.getSlideUploadEndTime());
	}

	/**
	 * 輔助方法：判斷目標時間是否在一個時間區間內 (包含起始時間，但不包含結束時間)。
	 * 這個邏輯常用於表示某個事件在指定結束時間點之前都有效，但到達或超過結束時間點後就無效。
	 *
	 * @param target 要檢查的目標時間。
	 * @param start  區間的起始時間 (包含)。
	 * @param end    區間的結束時間 (不包含)。
	 * @return 如果目標時間在區間內則返回 true，否則返回 false。
	 */
	private boolean isBetweenInclusiveStartExclusiveEnd(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
		// 呼叫此方法前，已經在各自的檢查方法中處理了 null 值判斷，因此這裡直接進行時間比較。
		return (target.isAfter(start) || target.isEqual(start)) && target.isBefore(end);
	}

}
