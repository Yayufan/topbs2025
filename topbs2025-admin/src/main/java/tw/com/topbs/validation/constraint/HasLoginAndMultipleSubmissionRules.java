package tw.com.topbs.validation.constraint;

import tw.com.topbs.enums.CommonStatusEnum;

public interface HasLoginAndMultipleSubmissionRules {

	public CommonStatusEnum getRequireLogin();
	public CommonStatusEnum getAllowMultipleSubmissions();
	
}
