package xlike.top.kn_ai_chat.handler;

import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.SystemService;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
@Component
public class SystemHandler implements MessageHandler {

    private final SystemService systemService;
    private final UserConfigService userConfigService;

    // 定义所有与SystemHandler相关的配置键
    private static final List<String> SYSTEM_HANDLER_KEYS = List.of(
            "SystemHandler_ClearHistory",
            "SystemHandler_QueryId",
            "SystemHandler_ChatStats",
            "SystemHandler_UserQuestions"
    );

    public SystemHandler(SystemService systemService, UserConfigService userConfigService) {
        this.systemService = systemService;
        this.userConfigService = userConfigService;
    }

    @Override
    public boolean canHandle(String content) {
        String lowerCaseContent = content.toLowerCase();
        
        // 动态地检查所有相关的关键词列表
        return SYSTEM_HANDLER_KEYS.stream()
                // a. 获取每个子命令的关键词列表 -> 得到一个 Stream<List<String>>
                .map(key -> userConfigService.getKeywordsForHandler("default", key))
                // b. 将多个List<String>拍平为一个大的Stream<String>
                .flatMap(List::stream)
                // c. 检查用户的输入是否与其中任何一个关键词匹配
                .anyMatch(lowerCaseContent::equalsIgnoreCase);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String response;
        String lowerCaseContent = content.toLowerCase();

        // 这里的逻辑保持不变，因为它是在canHandle返回true后才执行的
        if (userConfigService.getKeywordsForHandler("default", "SystemHandler_ClearHistory").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = systemService.clearHistory(externalUserId);
        } else if (userConfigService.getKeywordsForHandler("default", "SystemHandler_QueryId").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = "ℹ️ 您的用户ID是: " + externalUserId;
        } else if (userConfigService.getKeywordsForHandler("default", "SystemHandler_ChatStats").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = systemService.getChatStats(externalUserId);
        } else if (userConfigService.getKeywordsForHandler("default", "SystemHandler_UserQuestions").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = systemService.getUserQuestions(externalUserId);
        } else {
            return Optional.empty(); 
        }
        
        return Optional.of(new TextReply(response));
    }

    @Override
    public int getOrder() {
        return 2;
    }
}