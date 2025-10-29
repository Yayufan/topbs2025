package tw.com.topbs.convert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.com.topbs.enums.MemberCategoryEnum;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddAttendeesDTO;
import tw.com.topbs.pojo.VO.AttendeesTagVO;
import tw.com.topbs.pojo.VO.AttendeesVO;
import tw.com.topbs.pojo.entity.Attendees;
import tw.com.topbs.pojo.excelPojo.AttendeesExcel;

@Mapper(componentModel = "spring")
public interface AttendeesConvert {

	Attendees addDTOToEntity(AddAttendeesDTO addAttendeesDTO);

	// Attendees putDTOToEntity(PutAttendeesDTO putAttendeesDTO);

	AttendeesVO entityToVO(Attendees attendees);

	AttendeesTagVO entityToAttendeesTagVO(Attendees attendees);

	@Mapping(source = "attendeesId", target = "attendeesId", qualifiedByName = "convertLongToString")
	@Mapping(source = "member.memberId", target = "memberId", qualifiedByName = "convertLongToString")
	@Mapping(source = "member.idCard", target = "idCard")
	@Mapping(source = "member.chineseName", target = "chineseName")
	@Mapping(source = "member.groupCode", target = "groupCode")
	@Mapping(source = "member.groupRole", target = "groupRole")
	@Mapping(source = "member.email", target = "email")
	@Mapping(source = "member.title", target = "title")
	@Mapping(source = "member.firstName", target = "firstName")
	@Mapping(source = "member.lastName", target = "lastName")
	@Mapping(source = "member.country", target = "country")
	@Mapping(source = "member.remitAccountLast5", target = "remitAccountLast5")
	@Mapping(source = "member.affiliation", target = "affiliation")
	@Mapping(source = "member.jobTitle", target = "jobTitle")
	@Mapping(source = "member.phone", target = "phone")
	@Mapping(source = "member.receipt", target = "receipt")
	@Mapping(source = "member.food", target = "food")
	@Mapping(source = "member.foodTaboo", target = "foodTaboo")
	@Mapping(source = "member.category", target = "category", qualifiedByName = "convertCategory")
	@Mapping(source = "member.categoryExtra", target = "categoryExtra")
	@Mapping(source = "sequenceNo", target = "sequenceNo", qualifiedByName = "convertInteger2FormatString")
	AttendeesExcel voToExcel(AttendeesVO attendeesVO);

	@Named("convertCategory")
	default String convertCategory(Integer category) {
		return MemberCategoryEnum.fromValue(category).getLabelZh();
	}

	@Named("convertLongToString")
	default String convertLongToString(Long id) {
		return id.toString();
	}

	@Named("convertInteger2FormatString")
	default String convertInteger2FormatString(Integer sequenceNo) {
		if (sequenceNo != null) {
			return String.format("%03d", sequenceNo);
		}
		return null;
	}

}
