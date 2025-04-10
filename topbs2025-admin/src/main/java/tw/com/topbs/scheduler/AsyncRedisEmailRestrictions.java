package tw.com.topbs.scheduler;

import java.time.LocalDateTime;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class AsyncRedisEmailRestrictions {

	private static final String DAILY_EMAIL_QUOTA_KEY = "email:dailyQuota";

	//redLockClient01  businessRedissonClient
	@Qualifier("businessRedissonClient")
	private final RedissonClient redissonClient;

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
			System.out.println("AsyncRedisEmailRestrictions RedissonClient is indeed 'businessRedissonClient'.");
		} else {
			System.err.println("AsyncRedisEmailRestrictions RedissonClient is not 'businessRedissonClient'.");
		}
	}

	// 使用 Cron 表達式設置定時任務 (每分鐘第零秒執行此任務，測試時使用)
	//	@Scheduled(cron = "0 * * * * ?")
	// 使用 Cron 表達式設置定時任務 (每天凌晨2點執行 cron = "0 0 2 * * ?" )
	@Scheduled(cron = "0 0 2 * * ?")
	public void resetDailyEmailQuota() {
		RAtomicLong quota = redissonClient.getAtomicLong(DAILY_EMAIL_QUOTA_KEY);
		quota.set(300); // 設定配額為 300
		System.out.println("✅ 重設 Email 配額為 300 封：");
	}
}
