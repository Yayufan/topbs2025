package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tw.com.topbs.pojo.BO.MemberExcelRaw;
import tw.com.topbs.pojo.DTO.AddGroupMemberDTO;
import tw.com.topbs.pojo.DTO.AddMemberForAdminDTO;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.VO.MemberOrderVO;
import tw.com.topbs.pojo.VO.MemberTagVO;
import tw.com.topbs.pojo.VO.MemberVO;
import tw.com.topbs.pojo.entity.Member;
import tw.com.topbs.pojo.excelPojo.MemberExcel;

@Mapper(componentModel = "spring")
public interface MemberConvert {

	Member addDTOToEntity(AddMemberDTO addMemberDTO);

	Member addGroupDTOToEntity(AddGroupMemberDTO addGroupMemberDTO);

	Member forAdminAddDTOToEntity(AddMemberForAdminDTO addMemberForAdminDTO);

	Member putDTOToEntity(PutMemberDTO putMemberDTO);

	MemberVO entityToVO(Member member);

	List<MemberVO> entityListToVOList(List<Member> memberList);

	MemberTagVO entityToMemberTagVO(Member member);

	MemberOrderVO entityToMemberOrderVO(Member member);

	//實體類先轉成BO，這個BO之後要setStatus 手動塞入訂單狀態的
	MemberExcelRaw entityToExcelRaw(Member member);

	// BO對象轉成真正的Excel 對象
	@Mapping(target = "status", source = "status", qualifiedByName = "convertStatus")
	@Mapping(target = "category", source = "category", qualifiedByName = "convertCategory")
	MemberExcel memberExcelRawToExcel(MemberExcelRaw memberExcelRaw);

	@Named("convertStatus")
	default String convertStatus(Integer status) {
		switch (status) {
		case 0:
			return "未付款";
		case 1:
			return "付款-待確認";
		case 2:
			return "付款成功";
		case 3:
			return "付款失敗";
		default:
			return "未知";
		}
	}

	@Named("convertCategory")
	default String convertCategory(Integer category) {
		switch (category) {
		case 1:
			return "會員";
		case 2:
			return "其他";
		case 3:
			return "非會員";
		case 4:
			return "MVP";
		default:
			return "";
		}
	}

}
