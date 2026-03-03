package tw.com.topbs.convert;

import org.mapstruct.Mapper;

import tw.com.topbs.enums.CommonStatusEnum;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormDTO;
import tw.com.topbs.pojo.VO.FormVO;
import tw.com.topbs.pojo.entity.Form;

@Mapper(componentModel = "spring")
public interface FormConvert {

    // 宣告默認映射 , 告訴 MapStruct 如何把 CommonStatusEnum → Integer
    default Integer commonStatusEnumMapToInteger(CommonStatusEnum status) {
        return status == null ? null : status.getValue();
    }
	
	Form addDTOToEntity(AddFormDTO addFormDTO);
	
	Form putDTOToEntity(PutFormDTO putFormDTO);
	
	FormVO entityToVO(Form form);
	
}
