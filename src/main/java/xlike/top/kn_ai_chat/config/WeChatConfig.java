package xlike.top.kn_ai_chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

/**
 * RestTemplate 配置
 * @author Administrator
 */
@Configuration
public class WeChatConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        /*
         * 为 RestTemplate 配置UTF-8编码，解决发送中文乱码问题。
         * Spring 默认使用 StringHttpMessageConverter 来处理字符串类型（如我们的JSON字符串）的请求体，
         * 我们需要将其默认编码设置为 UTF-8。
         */
        restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}