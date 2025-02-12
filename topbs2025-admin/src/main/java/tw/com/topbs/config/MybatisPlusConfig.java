package tw.com.topbs.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

//設為配置類
@Configuration
//掃描任意層的Mapper資料夾
@MapperScan("tw.com.topbs.**.mapper")

public class MybatisPlusConfig {

	 /**
     * 添加分页插件
     * 分頁插件的實現是在送出SQL前,將他攔截並再次組裝,所以要先創建攔截器
     * 之後再添加插件到這個攔截器中,設置類型為MYSQL,並放置到IOC容器中
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
    	// 創建一個攔截器,往裡面填充插件,插件是可以疊加的
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 配置分頁插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 配置防全表更新与删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 配置樂觀鎖插件,避免修改衝突,也避免預設悲觀鎖的性能低下
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
    

    
    
	
}
