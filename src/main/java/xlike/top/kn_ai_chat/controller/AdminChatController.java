package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.ChatMessage;
import xlike.top.kn_ai_chat.domain.ManualTransferRequest;
import xlike.top.kn_ai_chat.repository.ChatMessageRepository;
import xlike.top.kn_ai_chat.repository.ManualTransferRepository;
import xlike.top.kn_ai_chat.service.WeChatKfAccountService;
import xlike.top.kn_ai_chat.service.WeChatService;

import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
@Controller
@RequestMapping("/admin")
public class AdminChatController {

    private final ManualTransferRepository requestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final WeChatService weChatService;
    private final StringRedisTemplate redisTemplate;
    private final WeChatKfAccountService weChatKfAccountService;

    public AdminChatController(ManualTransferRepository requestRepository,
                               ChatMessageRepository chatMessageRepository,
                               StringRedisTemplate redisTemplate,
                               WeChatService weChatService,
                               WeChatKfAccountService weChatKfAccountService) {
        this.requestRepository = requestRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.redisTemplate = redisTemplate;
        this.weChatService = weChatService;
        this.weChatKfAccountService = weChatKfAccountService;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    @GetMapping("/api/chat/pending-requests")
    @ResponseBody
    public ResponseEntity<List<ManualTransferRequest>> getPendingRequests(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(requestRepository.findByResolvedFalseOrderByRequestTimeDesc());
    }

    @GetMapping("/api/chat/history/{userId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String userId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(chatMessageRepository.findByExternalUserIdOrderByTimestampAsc(userId));
    }


    @PostMapping("/api/chat/end-service/{userId}")
    @ResponseBody
    public ResponseEntity<Void> endManualService(@PathVariable String userId, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }

        // 删除Redis中的人工模式标记，让用户回归AI服务
        String redisKey = "manual_chat_mode:" + userId;
        redisTemplate.delete(redisKey);
        // 将数据库中的请求标记为已解决
        Optional<ManualTransferRequest> requestOpt = requestRepository.findByExternalUserId(userId);
        requestOpt.ifPresent(request -> {
            request.setResolved(true);
            requestRepository.save(request);
        });
        // 主动推送一条消息给用户
        String openKfid = weChatKfAccountService.getOpenKfid();
        weChatService.sendTextMessage(userId, openKfid, "人工服务已结束，现在将由智能小助手继续为您服务。");
        return ResponseEntity.ok().build();
    }
}