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
    public boolean canHandle(String content, String externalUserId) {
        String lowerCaseContent = content.toLowerCase();
        
        return SYSTEM_HANDLER_KEYS.stream()
                .map(key -> userConfigService.getKeywordsForHandler(externalUserId, key))
                .flatMap(List::stream)
                .anyMatch(lowerCaseContent::equalsIgnoreCase);
    }

    /**
     * 修改了 handle 方法，将 "default" 替换为 externalUserId
     */
    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String response;
        String lowerCaseContent = content.toLowerCase();

        if (userConfigService.getKeywordsForHandler(externalUserId, "SystemHandler_ClearHistory").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = systemService.clearHistory(externalUserId);
        } else if (userConfigService.getKeywordsForHandler(externalUserId, "SystemHandler_QueryId").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = "ℹ️ 您的用户ID是: " + externalUserId;
        } else if (userConfigService.getKeywordsForHandler(externalUserId, "SystemHandler_ChatStats").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
            response = systemService.getChatStats(externalUserId);
        } else if (userConfigService.getKeywordsForHandler(externalUserId, "SystemHandler_UserQuestions").stream().anyMatch(lowerCaseContent::equalsIgnoreCase)) {
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