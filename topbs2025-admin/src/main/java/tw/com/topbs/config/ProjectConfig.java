package tw.com.topbs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "project")
public class ProjectConfig {
	
	private String domain;
	private String name;
	private String alias;
	private String bannerUrl;
	private Payment payment;
	private Integer groupSize;
	private Email email;

	@Getter
	@Setter
	public static class Payment {
		private String clientBackUrl;
		private String returnUrl;
		private String prefix;
	}

	@Getter
	@Setter
	public static class Email {
		private String from;
		private String fromName;
		private String replyTo;
	}
}
