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

    private static final String LIST_COMMAND = "列出文件";
    private static final String DELETE_COMMAND_PREFIX = "删除文件 ";
    private static final String DELETE_ALL_COMMAND = "删除所有文件";

    public KnowledgeHandler(KnowledgeBaseService knowledgeBaseService, AiService aiService, UserConfigService userConfigService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.aiService = aiService;
        this.userConfigService = userConfigService;
    }


    @Override
    public boolean canHandle(String content, String externalUserId) {
        String lowerCaseContent = content.toLowerCase();
        
        if (lowerCaseContent.equals(LIST_COMMAND.toLowerCase()) ||
            lowerCaseContent.startsWith(DELETE_COMMAND_PREFIX.toLowerCase()) ||
            lowerCaseContent.equalsIgnoreCase(DELETE_ALL_COMMAND)) {
            return true;
        }
        
        List<String> keywords = userConfigService.getKeywordsForHandler(externalUserId, this.getClass().getSimpleName());
        return keywords.stream().anyMatch(lowerCaseContent::contains);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        if (content.equalsIgnoreCase(LIST_COMMAND)) {
            logger.info("用户 [{}] 执行知识库指令: 列出文件", externalUserId);
            String fileList = knowledgeBaseService.getFormattedFileListForUser(externalUserId);
            return Optional.of(new TextReply(fileList));
        }
        
        if (content.equalsIgnoreCase(DELETE_ALL_COMMAND)) {
            logger.info("用户 [{}] 执行知识库指令: 删除所有文件", externalUserId);
            String result = knowledgeBaseService.deleteAllFilesForUser(externalUserId);
            return Optional.of(new TextReply(result));
        }

        if (content.toLowerCase().startsWith(DELETE_COMMAND_PREFIX.toLowerCase())) {
            logger.info("用户 [{}] 执行知识库指令: 删除文件", externalUserId);
            String idStr = content.substring(DELETE_COMMAND_PREFIX.length()).trim();
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