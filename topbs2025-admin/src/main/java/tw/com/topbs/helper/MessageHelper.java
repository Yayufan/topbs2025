package tw.com.topbs.helper;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.constant.I18nMessageKey;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageHelper {

	private final MessageSource messageSource;

	/**
	 * 獲取訊息（主要方法）
	 */
	public String get(String key, Object... args) {
		try {
			//			System.out.println("當前語系" + LocaleContextHolder.getLocale());
			return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
		} catch (NoSuchMessageException e) {
			log.warn("Message key not found: {}", key);
			// 沒找到符合的錯誤信息 , 直接顯示通用錯誤信息
			return messageSource.getMessage(I18nMessageKey.DEFAULT, args, LocaleContextHolder.getLocale());
		}
	}

	/**
	 * 獲取訊息帶默認值
	 */
	public String get(String key, String defaultMessage, Object... args) {
		return messageSource.getMessage(key, args, defaultMessage, LocaleContextHolder.getLocale());
	}

}
