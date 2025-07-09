package xlike.top.kn_ai_chat.handler;

import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.AdminService;

import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
@Component
public class UnifiedAdminHandler implements MessageHandler {

    private static final String AUTH_COMMAND = "/auth";
    private static final String CONFIG_COMMAND = "/set";
    // 新增：定义登出指令
    private static final String LOGOUT_COMMAND_1 = "/down";
    private static final String LOGOUT_COMMAND_2 = "/结束";
    // 强制指令
    private static final String FORCE_COMMAND = "#";

    private final AdminService adminService;

    public UnifiedAdminHandler(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public boolean canHandle(String content, String externalUserId) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String trimmedContent = content.trim();

        // 任何用户都可以尝试认证
        if (trimmedContent.startsWith(AUTH_COMMAND)) {
            return true;
        }
        
        // 只有管理员才能使用其他指令（包括登出）
        return adminService.isAdmin(externalUserId);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String trimmedContent = content.trim();

        // 优先处理认证指令
        if (trimmedContent.startsWith(AUTH_COMMAND)) {
            return handleAuthCommand(externalUserId, trimmedContent);
        }

        // 新增：处理登出指令
        if (trimmedContent.equals(LOGOUT_COMMAND_1) || trimmedContent.equals(LOGOUT_COMMAND_2)) {
            return Optional.of(adminService.logout(externalUserId));
        }
        
        // 处理设置指令
        if (trimmedContent.startsWith(CONFIG_COMMAND)) {
            return handleConfigCommand(trimmedContent);
        }

        // 强制指令
        if (trimmedContent.startsWith(FORCE_COMMAND)) {
            return handleToolExecution(externalUserId,trimmedContent);
        }

        // 如果不是以上特定指令，则视为通用的工具调用
        return handleToolExecution(externalUserId, trimmedContent);
    }

    private Optional<Reply> handleAuthCommand(String externalUserId, String content) {
        String[] parts = content.split("\\s+", 2);
        if (parts.length < 2) {
            return Optional.of(new TextReply("认证格式错误，请使用: /auth <密码>"));
        }
        String password = parts[1];
        return Optional.of(adminService.authenticate(externalUserId, password));
    }

    private Optional<Reply> handleConfigCommand(String content) {
        String[] parts = content.split("\\s+", 3);
        if (parts.length < 3) {
            return Optional.of(new TextReply("配置格式错误，请使用: /set <配置项> <配置值>"));
        }
        String key = parts[1];
        String value = parts[2];
        return Optional.of(adminService.updateConfig(key, value));
    }

    private Optional<Reply> handleToolExecution(String externalUserId, String content) {
        return Optional.of(adminService.executeTool(externalUserId, content));
    }
}