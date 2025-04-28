package tw.com.topbs.convert;

import java.util.List;

import org.mapstruct.Mapper;

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

	MemberExcel entityToExcel(MemberExcelRaw memberExcelRaw);




	
}
