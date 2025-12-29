package tw.com.topbs.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 的配置類, 這是用來處理傳送Long類型到前端時精度丟失的全局處理
 */
@Configuration
public class JacksonConfig {

	@Bean
	Jackson2ObjectMapperBuilder jacksonBuilder() {

		// 拿到Json 序列化 builder
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

		// Long 類型轉 String，避免前端精度丟失
		builder.serializerByType(Long.class, ToStringSerializer.instance);
		builder.serializerByType(Long.TYPE, ToStringSerializer.instance);

		// 註冊 全局 String 反序列化器(模組)：空字串轉 null
		SimpleModule stringModule = new SimpleModule();
		stringModule.addDeserializer(String.class, new JsonDeserializer<String>() {
			@Override
			public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
				String value = p.getValueAsString();
				return (value != null && value.isBlank()) ? null : value;
			}
		});
		
		
		// 讓builder使用這個模組,且不覆蓋自動加載的模組 , 例如 Java 8 日期時間模組
		builder.modulesToInstall(stringModule);

		return builder;
	}
	
	
	 
}
