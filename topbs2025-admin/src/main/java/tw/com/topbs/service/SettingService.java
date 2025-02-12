package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddSettingDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutSettingDTO;
import tw.com.topbs.pojo.entity.Setting;

public interface SettingService extends IService<Setting> {

	Setting getSetting(Long settingId);
	
	List<Setting> getSettingList();
	
	IPage<Setting> getSettingPage(Page<Setting> page);
	
	void addSetting(AddSettingDTO addSettingDTO);
	
	void updateSetting(PutSettingDTO putSettingDTO);
	
	void deleteSetting(Long settingId);
	
	void deleteSettingList(List<Long> settingIds);
	
}
