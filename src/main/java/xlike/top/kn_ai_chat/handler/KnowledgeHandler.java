package xlike.top.kn_ai_chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.AiService;
import xlike.top.kn_ai_chat.service.KnowledgeBaseService;
import xlike.top.kn_ai_chat.service.UserConfigService;

import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
@Component
public class KnowledgeHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeHandler.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final AiService aiService;
    private final UserConfigService userConfigService;

    public KnowledgeHandler(KnowledgeBaseService knowledgeBaseService, AiService aiService, UserConfigService userConfigService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.aiService = aiService;
        this.userConfigService = userConfigService;
    }

    @Override
    public boolean canHandle(String content, String externalUserId) {
        String lowerCaseContent = content.toLowerCase().trim();
        
        boolean isListCommand = userConfigService.getKeywordsForHandler(externalUserId, "KnowledgeHandler_List")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(lowerCaseContent));
        boolean isDeleteAllCommand = userConfigService.getKeywordsForHandler(externalUserId, "KnowledgeHandler_DeleteAll")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(lowerCaseContent));
        boolean isDeleteCommand = userConfigService.getKeywordsForHandler(externalUserId, "KnowledgeHandler_Delete")
                .stream().anyMatch(lowerCaseContent::startsWith);

        if (isListCommand || isDeleteAllCommand || isDeleteCommand) {
            return true;
        }
        
        List<String> keywords = userConfigService.getKeywordsForHandler(externalUserId, this.getClass().getSimpleName());
        return keywords.stream().anyMatch(lowerCaseContent::contains);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String trimmedContent = content.trim();

        boolean isListCommand = userConfigService.getKeywordsForHandler(externalUserId, "KnowledgeHandler_List")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmedContent));
        if (isListCommand) {
            logger.info("用户 [{}] 执行知识库指令: 列出文件", externalUserId);
            String fileList = knowledgeBaseService.getFormattedFileListForUser(externalUserId);
            return Optional.of(new TextReply(fileList));
        }
        
        boolean isDeleteAllCommand = userConfigService.getKeywordsForHandler(externalUserId, "KnowledgeHandler_DeleteAll")
                .stream().anyMatch(kw -> kw.equalsIgnoreCase(trimmedContent));
        if (isDeleteAllCommand) {
            logger.info("用户 [{}] 执行知识库指令: 删除所有文件", externalUserId);
            String result = knowledgeBaseService.deleteAllFilesForUser(externalUserId);
            return Optional.of(new TextReply(result));
        }

        Optional<String> deletePrefix = userConfigService.getKeywordsForHandler(externalUserId, "KnowledgeHandler_Delete")
                .stream().filter(trimmedContent::startsWith).findFirst();
        if (deletePrefix.isPresent()) {
            logger.info("用户 [{}] 执行知识库指令: 删除文件", externalUserId);
            String idStr = trimmedContent.substring(deletePrefix.get().length()).trim();
            try {
                long id = Long.parseLong(idStr);
                String result = knowledgeBaseService.deleteFileForUser(id, externalUserId);
                return Optional.of(new TextReply(result));
            } catch (NumberFormatException e) {
                return Optional.of(new TextReply("❌ 指令格式错误，请输入有效的数字ID。例如：删除文件 123"));
            }
        }

        logger.info("为用户 [{}] 的提问启用知识库增强问答 (关键词触发)...", externalUserId);
        String context = knowledgeBaseService.retrieveKnowledgeForUser(externalUserId);

        if (context == null || context.isBlank()) {
            return Optional.of(new TextReply("ℹ️ 您的知识库中还没有任何文件，请先上传文件再进行提问。"));
        }

        String answer = aiService.getChatCompletionWithContext(content, context, externalUserId, openKfid);
        return Optional.of(new TextReply(answer));
    }

    @Override
    public int getOrder() {
        return 5;
    }
}