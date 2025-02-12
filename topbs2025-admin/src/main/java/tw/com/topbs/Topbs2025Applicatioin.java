package tw.com.topbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("tw.com.topbs")
@EnableCaching
@SpringBootApplication
public class Topbs2025Applicatioin {
	public static void main(String[] args) {
		SpringApplication.run(Topbs2025Applicatioin.class, args);
	}
}
