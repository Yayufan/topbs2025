package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.google.common.base.Joiner;

import tw.com.topbs.pojo.DTO.addEntityDTO.AddPaperReviewerDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutPaperReviewerDTO;
import tw.com.topbs.pojo.entity.PaperReviewer;

@Mapper(componentModel = "spring")
public interface PaperReviewerConvert {

	@Mapping(target = "email", source = "emailList", qualifiedByName = "listToString")
	@Mapping(target = "absTypeList", source = "absTypeList", qualifiedByName = "listToString")
	PaperReviewer addDTOToEntity(AddPaperReviewerDTO addPaperReviewerDTO);

	@Mapping(target = "email", source = "emailList", qualifiedByName = "listToString")
	@Mapping(target = "absTypeList", source = "absTypeList", qualifiedByName = "listToString")
	PaperReviewer putDTOToEntity(PutPaperReviewerDTO putPaperReviewerDTO);

	@Named("listToString")
	default String listToString(List<String> strList) {
		return Joiner.on(",").skipNulls().join(strList);
	}

}
