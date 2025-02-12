package tw.com.topbs.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import cn.dev33.satoken.stp.SaTokenInfo;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddMemberDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutMemberDTO;
import tw.com.topbs.pojo.entity.Member;

public interface MemberService extends IService<Member> {

	Member getMember(Long memberId);

	List<Member> getMemberList();

	IPage<Member> getMemberPage(Page<Member> page);

	/**
	 * 新增會員，同時當作註冊功能使用，會自行產生會費訂單，且回傳tokenInfo
	 * 
	 * @param addMemberDTO
	 * @return
	 */
	SaTokenInfo addMember(AddMemberDTO addMemberDTO);

	void updateMember(PutMemberDTO putMemberDTO);

	void deleteMember(Long memberId);

	void deleteMemberList(List<Long> memberIds);
	
	Member getMemberInfo();

}
