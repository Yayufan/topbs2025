package tw.com.topbs.service;

import tw.com.topbs.pojo.entity.Member;

public interface AsyncService {

	void sendGroupRegistrationEmail(Member member) ;	
}
