package xlike.top.kn_ai_chat.handler;

import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.SystemService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 系统指令处理器
 * @author Administrator
 */
@Component
public class SystemHandler implements MessageHandler {

    private final SystemService systemService;

    // ▼▼▼【核心修改1】: 增加新的关键词 ▼▼▼
    private static final List<String> KEYWORDS = Arrays.asList(
            "清空历史对话", "清空记录","清除历史对话",
            "查询id", "我的id",
            "对话统计", "消息统计",
            "我问过的问题", "历史问题", "我的提问"
    );

    public SystemHandler(SystemService systemService) {
        this.systemService = systemService;
    }

    @Override
    public boolean canHandle(String content) {
        return KEYWORDS.stream().anyMatch(content::equalsIgnoreCase);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String response = switch (content.toLowerCase()) {
            case "清空历史对话", "清空记录" -> systemService.clearHistory(externalUserId);
            case "查询id", "我的id" -> "ℹ️ 您的用户ID是: " + externalUserId;
            case "对话统计", "消息统计" -> systemService.getChatStats(externalUserId);
            case "我问过的问题", "历史问题", "我的提问" -> systemService.getUserQuestions(externalUserId);
            default -> "❓ 未知的系统指令。";
        };
        // 使用 TextReply 包装返回结果
        return Optional.of(new TextReply(response));
    }

    @Override
    public int getOrder() {
        // 优先级设为2
        return 2;
    }
}