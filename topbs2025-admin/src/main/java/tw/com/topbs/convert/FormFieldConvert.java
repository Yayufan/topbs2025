package tw.com.topbs.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tw.com.topbs.enums.CommonStatusEnum;
import tw.com.topbs.pojo.DTO.FormFieldOptionDTO;
import tw.com.topbs.pojo.DTO.FormFieldValidationRuleDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutFormFieldOrderDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.entity.FormField;

@Mapper(componentModel = "spring")
public interface FormFieldConvert {

	ObjectMapper MAPPER = new ObjectMapper();
	
    // 宣告默認映射 , 告訴 MapStruct 如何把 CommonStatusEnum → Integer
    default Integer commonStatusEnumMapToInteger(CommonStatusEnum status) {
        return status == null ? null : status.getValue();
    }
    
    // 宣告默認映射 , 告訴 MapStruct 如何把 Integer → CommonStatusEnum
    default CommonStatusEnum commonStatusEnumMapToInteger(Integer status) {
        return CommonStatusEnum.fromValue(status);
    }
	
	@Mapping(target = "options", source = "options", qualifiedByName = "objectToJson")
    @Mapping(target = "validationRules", source = "validationRules", qualifiedByName = "objectToJson")
	FormField addDTOToEntity(AddFormFieldDTO addFormFieldDTO);
	
	@Mapping(target = "options", source = "options", qualifiedByName = "objectToJson")
    @Mapping(target = "validationRules", source = "validationRules", qualifiedByName = "objectToJson")
	FormField putDTOToEntity(PutFormFieldDTO putFormFieldDTO);
	
	FormField putOrderDTOToEntity(PutFormFieldOrderDTO putFormFieldOrderDTO);
	
	// Entity -> VO (JSON 字段反序列化為 DTO)
    @Mapping(target = "options", source = "options", qualifiedByName = "jsonToOptions")
    @Mapping(target = "validationRules", source = "validationRules", qualifiedByName = "jsonToValidationRules")
	FormFieldVO entityToVO(FormField formField);
	
	
    @Named("objectToJson")
    default String objectToJson(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert object to JSON", e);
        }
    }
    
    // 自定義 JSON -> DTO
    @Named("jsonToOptions")
    default FormFieldOptionDTO jsonToOptions(String json) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, FormFieldOptionDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse options JSON", e);
        }
    }

    @Named("jsonToValidationRules")
    default FormFieldValidationRuleDTO jsonToValidationRules(String json) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, FormFieldValidationRuleDTO.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse validationRules JSON", e);
        }
    }
	
}
