package xlike.top.kn_ai_chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.domain.McpConfig;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.McpService;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xlike
 */
@Component
public class McpHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(McpHandler.class);
    // 匹配 #mcp-序号 [问题]
    private static final Pattern MCP_EXEC_PATTERN = Pattern.compile("^#mcp-(\\d+)\\s+(.*)", Pattern.DOTALL);
    private final McpService mcpService;

    public McpHandler(McpService mcpService) {
        this.mcpService = mcpService;
    }

    @Override
    public boolean canHandle(String content, String externalUserId) {
        return content != null && content.trim().startsWith("#mcp");
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String trimmedContent = content.trim();
        // 场景一: 用户输入 #mcp-序号 执行指令
        Matcher execMatcher = MCP_EXEC_PATTERN.matcher(trimmedContent);
        if (execMatcher.matches()) {
            return handleMcpExecution(externalUserId, execMatcher);
        }
        // 场景二: 用户输入 #mcp 列出可用服务
        if (trimmedContent.equals("#mcp")) {
            return handleMcpListing(externalUserId);
        }
        // 场景三: 格式错误
        return Optional.of(new TextReply(getHelpText()));
    }

    /**
     * 处理列出可用 MCP 服务的请求
     */
    private Optional<Reply> handleMcpListing(String externalUserId) {
        List<McpConfig> authorizedConfigs = mcpService.getAndCacheAuthorizedMcpListForUser(externalUserId);
        if (authorizedConfigs.isEmpty()) {
            return Optional.of(new TextReply("您当前没有可用的 MCP 服务权限，请联系管理员。"));
        }
        StringBuilder replyText = new StringBuilder("您可使用的MCP服务列表:\n");
        for (int i = 0; i < authorizedConfigs.size(); i++) {
            replyText.append(String.format("%d. %s\n", i + 1, authorizedConfigs.get(i).getName()));
        }
        replyText.append("\n请使用 `#mcp-[序号] [您的问题]` 来调用服务。");
        replyText.append("\n例如: `#mcp-1 介绍一下北京`");

        return Optional.of(new TextReply(replyText.toString()));
    }

    /**
     * 处理执行 MCP 服务的请求
     */
    private Optional<Reply> handleMcpExecution(String externalUserId, Matcher matcher) {
        try {
            int index = Integer.parseInt(matcher.group(1));
            String prompt = matcher.group(2);
            Optional<McpConfig> mcpConfigOpt = mcpService.getMcpConfigFromCache(externalUserId, index);
            if (!mcpConfigOpt.isPresent()) {
                return Optional.of(new TextReply("❌ 无效的序号或会话已过期。\n请先发送 `#mcp` 获取最新的服务列表。"));
            }
            McpConfig config = mcpConfigOpt.get();
            String response = mcpService.executeMcpRequest(externalUserId, config.getName(), prompt);
            return Optional.of(new TextReply(response));
        } catch (NumberFormatException e) {
            // 这通常不会发生，因为正则表达式保证了是数字，但作为安全措施
            return Optional.of(new TextReply(getHelpText()));
        }
    }

    private String getHelpText() {
        return "❌ MCP 指令格式错误。\n\n" +
               "1. 发送 `#mcp` 获取您可用的服务列表。\n" +
               "2. 使用 `#mcp-[序号] [您的问题]` 来调用指定服务。";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}