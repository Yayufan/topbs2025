package tw.com.topbs.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

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

	/**
	 * 用於接收redis key 失效的監聽
	 * 其中redis.conf 要記得一項配置 notify-keyspace-events Ex ，要開啟Ex
	 * 
	 * @param connectionFactory
	 * @return
	 */
	@Bean
	RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {

		//Redis消息監聽器
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		//設置Redis鏈接工廠
		container.setConnectionFactory(connectionFactory);

		return container;
	}

}
