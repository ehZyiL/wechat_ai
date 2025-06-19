package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.WeChatUser;
import xlike.top.kn_ai_chat.repository.AiConfigRepository;
import xlike.top.kn_ai_chat.repository.MessageLogRepository;
import xlike.top.kn_ai_chat.repository.WeChatUserRepository;
import xlike.top.kn_ai_chat.service.KnowledgeBaseService;
import xlike.top.kn_ai_chat.service.SystemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xlike
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Value("${admin.password}")
    private String adminPassword;

    private final WeChatUserRepository userRepository;
    private final SystemService systemService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final MessageLogRepository messageLogRepository;
    private final AiConfigRepository aiConfigRepository;

    // 【新增】用于返回给前端的DTO，包含用户和提问总数
    @Data
    @AllArgsConstructor
    public static class UserStatsDto {
        private WeChatUser user;
        private long totalQuestions;
    }

    // 【修改】构造函数，注入所有依赖
    public AdminController(WeChatUserRepository userRepository, SystemService systemService, KnowledgeBaseService knowledgeBaseService, MessageLogRepository messageLogRepository, AiConfigRepository aiConfigRepository) {
        this.userRepository = userRepository;
        this.systemService = systemService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.messageLogRepository = messageLogRepository;
        this.aiConfigRepository = aiConfigRepository;
    }

    // ... (login, userManagementPage, getAllUsers 等方法保持不变)
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String password, HttpSession session, Model model) {
        if (adminPassword.equals(password)) {
            session.setAttribute("isAdmin", true);
            return "redirect:/admin/users";
        } else {
            model.addAttribute("error", "密码错误！");
            return "login";
        }
    }
    
    @GetMapping("/users")
    public String userManagementPage(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        return "users";
    }
    
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<UserStatsDto>> getAllUsers(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return ResponseEntity.status(401).build();
        }
        List<WeChatUser> users = userRepository.findAll();
        // 将WeChatUser列表转换为UserStatsDto列表
        List<UserStatsDto> userStats = users.stream()
                .map(user -> new UserStatsDto(
                        user,
                        systemService.countQuestionsFromUser(user.getExternalUserId())
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(userStats);
    }

    @PostMapping("/api/users/{userId}/clear-history")
    @ResponseBody
    public ResponseEntity<Void> clearUserHistory(@PathVariable String userId, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return ResponseEntity.status(401).build();
        }
        systemService.clearHistory(userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 【新增】彻底删除用户及其所有数据的API端点
     */
    @DeleteMapping("/api/users/{userId}")
    @ResponseBody
    @Transactional // 将所有数据库操作包含在一个事务中，确保原子性
    public ResponseEntity<Void> deleteUser(@PathVariable String userId, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return ResponseEntity.status(401).build();
        }
        logger.warn("接收到删除用户 [{}] 的高危操作请求", userId);

        // 步骤1: 删除对话记录
        logger.info("正在删除用户 [{}] 的对话记录...", userId);
        messageLogRepository.deleteByFromUserOrToUser(userId, userId);
        logger.info("用户 [{}] 的对话记录已删除。", userId);

        // 步骤2: 删除知识库文件和记录
        logger.info("正在删除用户 [{}] 的知识库文件...", userId);
        knowledgeBaseService.deleteKnowledgeByUserId(userId);
        logger.info("用户 [{}] 的知识库文件已删除。", userId);

        // 步骤3: 删除用户的AI个性化配置
        logger.info("正在删除用户 [{}] 的AI配置...", userId);
        aiConfigRepository.deleteByExternalUserId(userId);
        logger.info("用户 [{}] 的AI配置已删除。", userId);

        // 步骤4: 删除用户主体记录
        logger.info("正在删除用户 [{}] 的主体记录...", userId);
        userRepository.deleteById(userId);
        
        logger.warn("已彻底删除用户 [{}] 及其所有关联数据。", userId);

        return ResponseEntity.ok().build();
    }


    // ... (block/unblock 等方法保持不变)
    @PostMapping("/api/users/{userId}/block")
    @ResponseBody
    public ResponseEntity<Void> blockUser(@PathVariable String userId, HttpSession session) {
        return toggleBlockStatus(userId, true, session);
    }
    
    @PostMapping("/api/users/{userId}/unblock")
    @ResponseBody
    public ResponseEntity<Void> unblockUser(@PathVariable String userId, HttpSession session) {
        return toggleBlockStatus(userId, false, session);
    }

    private ResponseEntity<Void> toggleBlockStatus(String userId, boolean blocked, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findById(userId)
                .map(user -> {
                    user.setBlocked(blocked);
                    user.setLastUpdated(LocalDateTime.now());
                    userRepository.save(user);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}