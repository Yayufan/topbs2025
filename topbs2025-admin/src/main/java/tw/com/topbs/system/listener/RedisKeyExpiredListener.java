package tw.com.topbs.system.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

	public RedisKeyExpiredListener(RedisMessageListenerContainer listenerContainer) {
		super(listenerContainer);
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		System.out.println("redis過期的key為: " + expiredKey);
		if (expiredKey.startsWith("chunk:")) {
			//            String fileMd5 = expiredKey.substring("chunk:".length());
			//            System.out.println("檔案過期，fileSHA256 = " + fileMd5);

			// TODO: 呼叫 MinIO 刪除 chunk，刪資料庫、log 等
		}
	}

}
