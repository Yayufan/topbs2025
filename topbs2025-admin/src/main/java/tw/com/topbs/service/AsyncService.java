package tw.com.topbs.service;

import tw.com.topbs.pojo.entity.Member;

public interface AsyncService {
	
	void sendCommonEmail(String to,String subject,String htmlContent,String plainTextContent);

	void sendGroupRegistrationEmail(Member member) ;	
	
}
