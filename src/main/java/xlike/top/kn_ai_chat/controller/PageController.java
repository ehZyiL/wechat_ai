package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Administrator
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/admin/login";
    }

    @GetMapping("/config")
    public String configPage(@RequestParam(required = false) String externalUserId, Model model, HttpSession session) {
        // 检查 session 中是否有登录标记
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        
        model.addAttribute("externalUserId", externalUserId != null ? externalUserId : "");
        return "config";
    }
    
    /**
     * 【新增】文件管理页面路由
     */
    @GetMapping("/files")
    public String filesPage(@RequestParam String externalUserId, Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        model.addAttribute("externalUserId", externalUserId);
        return "files";
    }
}