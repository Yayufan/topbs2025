package tw.com.topbs.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    // 你可以自定義線程池配置
    @Bean(name = "taskExecutor")
    Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心線程數
        executor.setCorePoolSize(10);
        //線程池最大線程數
        executor.setMaxPoolSize(30);
        //消息對列最大儲存數
        executor.setQueueCapacity(100);
        //線程前墜
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
