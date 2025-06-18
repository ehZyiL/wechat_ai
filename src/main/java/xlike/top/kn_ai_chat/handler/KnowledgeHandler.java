package xlike.top.kn_ai_chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.AiService;
import xlike.top.kn_ai_chat.service.KnowledgeBaseService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 知识库处理器
 * <p>
 * 1. 响应知识库管理指令 ("列出文件", "删除所有文件", "删除文件 [ID]").
 * 2. 【新】仅当用户消息包含 "知识库" 关键词时，才执行RAG问答.
 *
 * @author Administrator
 */
@Component
public class KnowledgeHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeHandler.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final AiService aiService;

    // --- 指令定义 ---
    private static final String KEYWORD_TRIGGER = "知识库";
    private static final String LIST_COMMAND = "列出文件";
    private static final List<String> LIST_ALIASES = Arrays.asList("我的知识库", "知识库列表", "查看文件");
    private static final String DELETE_COMMAND_PREFIX = "删除文件 ";
    private static final String DELETE_ALL_COMMAND = "删除所有文件";

    public KnowledgeHandler(KnowledgeBaseService knowledgeBaseService, AiService aiService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.aiService = aiService;
    }

    /**
     * 【已修改】只有在消息内容包含相关指令时，此Handler才响应
     */
    @Override
    public boolean canHandle(String content) {
        String lowerCaseContent = content.toLowerCase();
        return lowerCaseContent.contains(KEYWORD_TRIGGER) ||
               lowerCaseContent.equals(LIST_COMMAND) ||
               LIST_ALIASES.stream().anyMatch(lowerCaseContent::equalsIgnoreCase) ||
               lowerCaseContent.startsWith(DELETE_COMMAND_PREFIX) ||
               lowerCaseContent.equalsIgnoreCase(DELETE_ALL_COMMAND);
    }

    /**
     * 【已修改】重构处理逻辑，精确匹配指令
     */
    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        // 1. 处理特定的知识库管理指令
        if (content.equalsIgnoreCase(LIST_COMMAND) || LIST_ALIASES.stream().anyMatch(content::equalsIgnoreCase)) {
            logger.info("用户 [{}] 执行知识库指令: 列出文件", externalUserId);
            String fileList = knowledgeBaseService.listFilesForUser(externalUserId);
            return Optional.of(new TextReply(fileList));
        }
        
        if (content.equalsIgnoreCase(DELETE_ALL_COMMAND)) {
            logger.info("用户 [{}] 执行知识库指令: 删除所有文件", externalUserId);
            String result = knowledgeBaseService.deleteAllFilesForUser(externalUserId);
            return Optional.of(new TextReply(result));
        }

        if (content.toLowerCase().startsWith(DELETE_COMMAND_PREFIX)) {
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

        // 2. 【核心修改】只在包含特定关键词时，才执行 RAG 逻辑
        if (content.contains(KEYWORD_TRIGGER)) {
            logger.info("为用户 [{}] 的提问启用知识库增强问答 (关键词触发)...", externalUserId);
            String context = knowledgeBaseService.retrieveKnowledgeForUser(externalUserId);

            if (context == null || context.isBlank()) {
                return Optional.of(new TextReply("ℹ️ 您的知识库中还没有任何文件，请先上传文件再进行提问。"));
            }

            String answer = aiService.getChatCompletionWithContext(content, context, openKfid);
            return Optional.of(new TextReply(answer));
        }

        // 3. 如果不满足任何条件，则不处理，交由下一个处理器
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        // 优先级设置为 5，确保在大多数特定指令处理器之后，但在最终的通用 AiMessageHandler 之前执行
        return 5;
    }
}