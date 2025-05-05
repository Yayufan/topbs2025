package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddSettingDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutSettingDTO;
import tw.com.topbs.pojo.VO.SettingVO;
import tw.com.topbs.pojo.entity.Setting;

@Mapper(componentModel = "spring")
public interface SettingConvert {

	Setting addDTOToEntity(AddSettingDTO addSettingDTO);

	Setting putDTOToEntity(PutSettingDTO putSettingDTO);
	
	SettingVO entityToVO(Setting setting);
	
	List<SettingVO> entityListToVOList(List<Setting> settingList);
	
}
