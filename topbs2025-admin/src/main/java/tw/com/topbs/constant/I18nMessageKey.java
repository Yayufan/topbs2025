package tw.com.topbs.constant;

import lombok.Getter;
import lombok.Setter;


/**
 * I18N 信息key <br>
 * 會與 src/main/resources 裡的 messages_*.properties 匹配
 * 
 */
@Getter
@Setter
public class I18nMessageKey {
	// 通用錯誤key
	public static final String DEFAULT = "default";

	// 註冊相關key
	public static final class Registration {
		public static final String CLOSED = "registration.closed";
		
		// 團體註冊
		public static final class Group {
			public static final String CLOSED = "registration.group.closed";
		}
		
	    // 帳號驗證相關
	    public static final class Auth {
	        public static final String EMAIL_NOT_FOUND = "registration.auth.email-not-found";
	        public static final String EMAIL_REGISTERED = "registration.auth.email-registered";
	        public static final String WRONG_ACCOUNT = "registration.auth.wrong-account";

	    }

	}

	// 付款相關key
	public static final class Payment {
		
		public static final String CLOSED = "payment.closed";

		// 團體付款
		public static final class Group {
			public static final String CLOSED = "payment.group.closed";
			public static final String MUST_BE_PRIMARY = "payment.group.must-be-primary";
		}

	}

	// 投稿相關的key
	public static final class Paper {
		public static final String CLOSED = "paper.closed";
		public static final String NO_MATCH = "paper.no-match";
		public static final String PREPAID = "paper.prepaid";

		// 投稿附件相關的key
		public static final class Attachment {
			public static final String NO_MATCH = "paper.attachment.no-match";
			public static final String FILE_SIZE = "paper.attachment.file-size";
			public static final String FILE_TYPE = "paper.attachment.file-type";
		}
	}
}
