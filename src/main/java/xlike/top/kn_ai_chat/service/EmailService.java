package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendEmail(String to, String subject, String body) {
        logger.info("--- 模拟邮件发送 ---");
        logger.info("收件人: {}", to);
        logger.info("主题: {}", subject);
        logger.info("内容: {}", body);
        logger.info("--- 邮件发送结束 ---");
    }
}