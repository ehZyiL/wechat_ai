package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.McpAiConfig;
import xlike.top.kn_ai_chat.domain.WeChatUser;
import xlike.top.kn_ai_chat.repository.WeChatUserRepository;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/admin/api/mcp-ai-config")
public class McpAiConfigController {

    private final UserConfigService userConfigService;
    private final WeChatUserRepository weChatUserRepository;

    public McpAiConfigController(UserConfigService userConfigService, WeChatUserRepository weChatUserRepository) {
        this.userConfigService = userConfigService;
        this.weChatUserRepository = weChatUserRepository;
    }

    private boolean isAdmin(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute("isAdmin"));
    }

    /**
     * 获取所有MCP AI配置 以及所有用户信息
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllConfigsWithUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }

        // 获取所有配置，并将其放入一个以userId为键的Map中
        Map<String, McpAiConfig> configsMap = userConfigService.getAllMcpAiConfigs().stream()
                .collect(Collectors.toMap(McpAiConfig::getExternalUserId, c -> c));
        
        // 获取所有微信用户
        List<WeChatUser> users = weChatUserRepository.findAll();

        // 将'default'配置也当作一个特殊用户返回，方便前端处理
        McpAiConfig defaultConfig = configsMap.get("default");
        
        Map<String, Object> response = Map.of(
            "configs", configsMap,
            "users", users,
            "defaultConfig", defaultConfig != null ? defaultConfig : new McpAiConfig()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 保存或更新一个MCP AI配置
     */
    @PostMapping("/save")
    public ResponseEntity<?> saveConfig(@RequestBody McpAiConfig config, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        try {
            McpAiConfig savedConfig = userConfigService.saveOrUpdateMcpAiConfig(config);
            return ResponseEntity.ok(savedConfig);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "保存失败: " + e.getMessage()));
        }
    }

    /**
     * 重置用户的MCP AI配置（即删除用户的特定配置，使其回退到default）
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetConfig(@RequestBody Map<String, String> payload, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(401).build();
        }
        String userId = payload.get("externalUserId");
        if (userId == null || "default".equals(userId)) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "不能重置default配置或无效的用户ID。"));
        }
        userConfigService.resetMcpAiConfig(userId);
        return ResponseEntity.ok(Collections.singletonMap("message", "用户 " + userId + " 的配置已重置。"));
    }
}