package tw.com.topbs.service;

import tw.com.topbs.pojo.DTO.EmailBodyContent;

public interface NotificationService {

	EmailBodyContent generateSpeakerUpdateContent(String speakerName, String adminDashboardUrl);
}
