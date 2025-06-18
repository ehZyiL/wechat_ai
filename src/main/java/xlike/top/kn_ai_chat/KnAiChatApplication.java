package xlike.top.kn_ai_chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Administrator
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class KnAiChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnAiChatApplication.class, args);
    }

}
