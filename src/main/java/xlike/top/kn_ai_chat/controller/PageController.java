package xlike.top.kn_ai_chat.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author xlike
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/admin/login";
    }

    @GetMapping("/config")
    public String configPage(@RequestParam(required = false) String externalUserId, Model model, HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        
        model.addAttribute("externalUserId", externalUserId != null ? externalUserId : "");
        return "config";
    }

    /**
     * MCP管理页面路由
     */
    @GetMapping("/admin/mcp-management")
    public String mcpManagementPage(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        return "mcp-management";
    }

    /**
     * MCP配置页面路由
     */
    @GetMapping("/admin/mcp-ai-config")
    public String mcpConfigPage(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("isAdmin"))) {
            return "redirect:/admin/login";
        }
        return "mcp-ai-config";
    }
    
    /**
     * 文件管理页面路由
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