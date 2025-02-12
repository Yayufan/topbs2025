package tw.com.topbs.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RedissonConfig {

	@Value("${redisson.address}")
	private String redissonAddress;

	@Value("${redisson.database}")
	private int database;

	// 業務(鑑權)用的Redis (Redisson客戶端)
//	@Bean(name="businessRedissonClient")
	@Bean
	// 指定主業務需要用到的redis
	@Primary
	RedissonClient businessRedissonClient() {
		Config config = new Config();
		// 配置單實例redis , 同時這也是分佈式鎖建議的創建客戶端方式,
		config.useSingleServer().setAddress(redissonAddress).setDatabase(database);
		return Redisson.create(config);
	}

	// 分佈式鎖用的Redis (Redisson客戶端)
//	@Bean(name="redLockClient01")
	@Bean
	RedissonClient redLockClient01() {
		Config config = new Config();
		// 配置單實例redis , 同時這也是分佈式鎖建議的創建客戶端方式,
		// 分佈式鎖下,要實現紅鎖(RedLock)每台redis都得是master
		config.useSingleServer().setAddress(redissonAddress).setDatabase(database);
		return Redisson.create(config);
	}

}
