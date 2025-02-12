package tw.com.topbs.utils;

import java.util.stream.StreamSupport;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 文章計數器 工具類
 * </p>
 *
 * @author Joey
 * @since 2024-09-10
 */
@Component
@RequiredArgsConstructor()
public class ArticleViewsCounterUtil {

	@Qualifier("businessRedissonClient") 
	private final RedissonClient redissonClient;
	
	private final String keyPrefix = "Article";
	
	/**
	 * 初始化測試使用spring 上下文
	 */
	
	@Autowired
	private ApplicationContext context;
	
	/**
	 * 初始化測試,這個是用來判斷 lombok.config 搭配 @Qualifier是有生效的 
	 * 
	 * 
	 */
	
	@PostConstruct
	public void init() {
	    Object proxy = context.getAutowireCapableBeanFactory().getBean("businessRedissonClient");
	    if (proxy == redissonClient) {
	        System.out.println("ArticleViewsCounterUtil redissonClient is indeed 'businessRedissonClient'.");
	    } else {
	        System.err.println("ArticleViewsCounterUtil redissonClient is not 'businessRedissonClient'.");
	    }
	}
	
	
	/**
	 * 提供類別及文章id, 將此文章放入redis 計數器,文章瀏覽量+1
	 * 
	 * @param category
	 * @param id
	 */
	public void incrementViewCount(String category, Long id) {
		String key = buildKey(category, id);
		RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
		atomicLong.incrementAndGet();
	}

	/**
	 * 提供類別及文章id, 找到單一文章的瀏覽量
	 * 
	 * @param category
	 * @param id
	 * @return 單一文章的瀏覽量
	 */
	public Long getViewCount(String category, Long id) {
		String key = buildKey(category, id);
		RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
		return atomicLong.get();
	}

	/**
	 * 獲取指定類別的所有文章瀏覽量總和
	 * 
	 * @param category
	 * @return 總瀏覽量
	 */
	public Long getTotalViewCount(String category) {
		// 組裝政則表達式
		String pattern = keyPrefix + ":" + category + ":views:*";
		RKeys keys = redissonClient.getKeys();

		// spliterator() 是 Iterable 接口的一个默認方法，它返回一個 Spliterator 對象。
		// Spliterator 是"可分割迭代器"的意思，它是為了並行處理而設計的，它知道如何將一個數據源分割成多個部分。

		// 使用 StreamSupport.stream() 來從 Iterable 創建一个 Stream：
		// StreamSupport.stream(keyIterable.spliterator(), false) 將 Iterable<String>
		// 轉換為Stream<String>。
		// 第二個參數 false 表示我们不需要並行流處理。

		// 使用 mapToLong 來獲取每個鍵對應的瀏覽量。然後使用 sum() 方法来計算總和。

		Iterable<String> keyIterable = keys.getKeysByPattern(pattern);
		return StreamSupport.stream(keyIterable.spliterator(), false)
				.mapToLong(key -> redissonClient.getAtomicLong(key).get()).sum();
	}

	private String buildKey(String category, Long id) {
		return keyPrefix + ":" + category + ":" + "views" + ":" + id;
	}

	/**
	 * 斷開redisson客戶端連結，基本上不要在業務層使用
	 */
	public void close() {
		redissonClient.shutdown();
	}

}
