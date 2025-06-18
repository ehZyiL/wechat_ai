package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xlike.top.kn_ai_chat.domain.WeChatUser;
import xlike.top.kn_ai_chat.repository.WeChatUserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Administrator
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Value("${admin.password}")
    private String adminPassword;

    private final WeChatUserRepository userRepository;

    public AdminController(WeChatUserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
    
    // --- 用户管理页面 ---
    @GetMapping("/users")
    public String userManagementPage(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        return "users";
    }

    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<WeChatUser>> getAllUsers(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/api/users/{userId}/block")
    @ResponseBody
    public ResponseEntity<Void> blockUser(@PathVariable String userId, HttpSession session) {
        // 调用重构后的通用方法，传入 true 表示拉黑
        return toggleBlockStatus(userId, true, session);
    }
    
    /**
     * 【新增】处理解禁用户的请求
     */
    @PostMapping("/api/users/{userId}/unblock")
    @ResponseBody
    public ResponseEntity<Void> unblockUser(@PathVariable String userId, HttpSession session) {
        // 调用重构后的通用方法，传入 false 表示解禁
        return toggleBlockStatus(userId, false, session);
    }

    /**
     * 【重构】通用的切换用户拉黑状态的方法
     * @param userId 用户ID
     * @param blocked true为拉黑, false为解禁
     * @param session HTTP会话
     * @return 操作结果
     */
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