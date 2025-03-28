package tw.com.topbs.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

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
        
        
        // 核心線程數 (根據服務器核心數調整)
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        // 線程池最大線程數
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 3);
        // 隊列容量
        executor.setQueueCapacity(500);
        // 線程名前綴
        executor.setThreadNamePrefix("ZipStreaming-");
        // 拒絕策略 - 由調用者線程運行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 線程空閒時間
        executor.setKeepAliveSeconds(60);
        // 等待所有任務完成再關閉
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待終止時間
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        
//        //核心線程數
//        executor.setCorePoolSize(10);
//        //線程池最大線程數
//        executor.setMaxPoolSize(30);
//        //消息對列最大儲存數
//        executor.setQueueCapacity(100);
//        //線程前墜
//        executor.setThreadNamePrefix("Async-");
//        executor.initialize();
        
        
        
        return executor;
    }
}
