package xlike.top.kn_ai_chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature; // 导入新模块
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.ChatMessage;
import xlike.top.kn_ai_chat.domain.ManualTransferRequest;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.handler.websocket.AdminWebSocketHandler;
import xlike.top.kn_ai_chat.repository.ChatMessageRepository;
import xlike.top.kn_ai_chat.repository.ManualTransferRepository;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.EmailService;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author xlike
 */
@Component
public class ManualChatHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(ManualChatHandler.class);
    private static final String MANUAL_MODE_KEY_PREFIX = "manual_chat_mode:";
    private static final long MANUAL_MODE_TIMEOUT_MINUTES = 30;

    private final StringRedisTemplate redisTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final ManualTransferRepository requestRepository;
    private final EmailService emailService;
    private final AdminWebSocketHandler adminWebSocketHandler;
    private final ObjectMapper objectMapper;
    private final UserConfigService userConfigService; // 新增

    public ManualChatHandler(StringRedisTemplate redisTemplate,
                             ChatMessageRepository chatMessageRepository,
                             ManualTransferRepository requestRepository,
                             EmailService emailService,
                             @Lazy AdminWebSocketHandler adminWebSocketHandler,
                             UserConfigService userConfigService) { // 新增
        this.redisTemplate = redisTemplate;
        this.chatMessageRepository = chatMessageRepository;
        this.requestRepository = requestRepository;
        this.emailService = emailService;
        this.adminWebSocketHandler = adminWebSocketHandler;
        this.userConfigService = userConfigService; // 新增
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳（数组）的特性，强制其输出为ISO-8601标准字符串
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private String getRedisKey(String externalUserId) {
        return MANUAL_MODE_KEY_PREFIX + externalUserId;
    }

    @Override
    public boolean canHandle(String content, String externalUserId) {
        String trimmedContent = content.trim();
        boolean isEnter = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Enter")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmedContent));
        boolean isExit = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Exit")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmedContent));
        return isEnter || isExit || isInManualMode(externalUserId);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String redisKey = getRedisKey(externalUserId);
        String trimmedContent = content.trim();

        boolean isExit = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Exit")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmedContent));
        if (isExit) {
            redisTemplate.delete(redisKey);
            Optional<ManualTransferRequest> requestOpt = requestRepository.findByExternalUserId(externalUserId);
            requestOpt.ifPresent(request -> {
                request.setResolved(true);
                requestRepository.save(request);
            });
            logger.info("用户 [{}] 主动结束人工服务，并已将请求标记为已解决。", externalUserId);
            return Optional.of(new TextReply("您已结束人工服务，现在将由智能小助手继续为您服务。"));
        }

        boolean isEnter = userConfigService.getKeywordsForHandler(externalUserId, "ManualChatHandler_Enter")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmedContent));
        if (isEnter) {
            redisTemplate.opsForValue().set(redisKey, "true", MANUAL_MODE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            createOrUpdateManualTransferRequest(externalUserId, content);
            logger.info("用户 [{}] 进入人工服务模式，有效期 {} 分钟。", externalUserId, MANUAL_MODE_TIMEOUT_MINUTES);
            return Optional.of(new TextReply("您好，已为您转接人工客服，目前人工客服暂时只能处理文字形式的问题，请直接发送您的问题。如需结束，请发送“结束人工服务”。"));
        }

        if (isInManualMode(externalUserId)) {
            logger.info("捕获用户 [{}] 的人工模式消息: {}", externalUserId, content);
            
            ChatMessage chatMessage = saveUserMessage(externalUserId, content);
            
            try {
                String jsonMessage = objectMapper.writeValueAsString(chatMessage);
                adminWebSocketHandler.sendMessageToAdmins(jsonMessage);
                logger.info("已将用户 [{}] 的消息推送给所有在线管理员。", externalUserId);
            } catch (Exception e) {
                logger.error("序列化或推送用户消息到管理员时出错", e);
            }

            redisTemplate.expire(redisKey, MANUAL_MODE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            return Optional.empty(); 
        }

        return Optional.empty();
    }

    private boolean isInManualMode(String externalUserId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getRedisKey(externalUserId)));
    }
    
    private void createOrUpdateManualTransferRequest(String externalUserId, String content) {
        ManualTransferRequest request = requestRepository.findByExternalUserId(externalUserId)
                .orElse(new ManualTransferRequest()); 

        request.setExternalUserId(externalUserId);
        request.setLastMessage(content);
        request.setRequestTime(LocalDateTime.now());
        request.setResolved(false);
        requestRepository.save(request);

        if (request.getId() == null) {
            emailService.sendEmail("admin@example.com", "新的人工客服请求",
                    "用户 " + externalUserId + " 请求人工服务。");
        }
    }

    private ChatMessage saveUserMessage(String externalUserId, String content) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setExternalUserId(externalUserId);
        chatMessage.setSenderType(ChatMessage.SenderType.USER);
        chatMessage.setMessageType(ChatMessage.MessageType.TEXT);
        chatMessage.setContent(content);
        chatMessage.setTimestamp(LocalDateTime.now());
        return chatMessageRepository.save(chatMessage);
    }
}