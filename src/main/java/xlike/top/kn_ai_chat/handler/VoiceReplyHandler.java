package xlike.top.kn_ai_chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.enums.MediaType;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.reply.VoiceReply;
import xlike.top.kn_ai_chat.service.*;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 强制语音回复处理器
 * <p>
 * 当用户消息中包含特定关键词时，此处理器将触发。
 * 它会先获取AI的文本回复，然后将该文本转换为语音并发送。
 * @author Administrator
 */
@Component
public class VoiceReplyHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceReplyHandler.class);

    /**
     * ▼▼▼【核心修改】▼▼▼
     * 将单个关键词改为一个列表，可以方便地增加更多触发词。
     */
    private static final List<String> TRIGGER_KEYWORDS = List.of(
            "语音回答",
            "语音回复",
            "用语音说",
            "讲一下"
    );

    private final AiService aiService;
    private final SiliconFlowService siliconFlowService;
    private final MediaService mediaService;
    private final FormatFileService formatFileService;

    public VoiceReplyHandler(AiService aiService, SiliconFlowService siliconFlowService, MediaService mediaService, FormatFileService formatFileService) {
        this.aiService = aiService;
        this.siliconFlowService = siliconFlowService;
        this.mediaService = mediaService;
        this.formatFileService = formatFileService;
    }

    /**
     * ▼▼▼【核心修改】▼▼▼
     * 判断消息内容是否包含列表中的任意一个关键词。
     */
    @Override
    public boolean canHandle(String content) {
        return TRIGGER_KEYWORDS.stream().anyMatch(content::contains);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String actualQuery = content;
        String foundKeyword = "";

        // ▼▼▼【核心修改】▼▼▼
        // 查找具体命中了哪个关键词，并将其从内容中移除，以获得真正的提问
        for (String keyword : TRIGGER_KEYWORDS) {
            if (content.contains(keyword)) {
                foundKeyword = keyword;
                actualQuery = content.replace(keyword, "").trim();
                break; // 找到第一个匹配的关键词就停止
            }
        }

        if (actualQuery.isEmpty()) {
            return Optional.of(new TextReply("您想让我用语音回答什么问题呢？"));
        }

        logger.info("接收到强制语音回复指令，关键词: '{}', 问题: {}", foundKeyword, actualQuery);

        // 后续的逻辑保持不变
        List<MessageLog> queryHistory = new java.util.ArrayList<>(history);
        queryHistory.get(queryHistory.size() - 1).setContent(actualQuery);
        String textReply = aiService.getChatCompletion(queryHistory, openKfid);

        if (textReply == null || textReply.isEmpty()) {
            return Optional.of(new TextReply("抱歉，我暂时无法回答这个问题。"));
        }

        Optional<File> mp3FileOpt = siliconFlowService.generateSpeech(textReply);
        if (mp3FileOpt.isEmpty()) {
            return Optional.of(new TextReply("抱歉，语音文件生成失败。给您文字版回复：\n\n" + textReply));
        }

        File mp3File = mp3FileOpt.get();
        Optional<File> amrFileOpt = formatFileService.convertToAmr(mp3File);
        mp3File.delete();

        if (amrFileOpt.isEmpty()) {
            return Optional.of(new TextReply("抱歉，语音格式转换失败。给您文字版回复：\n\n" + textReply));
        }
        
        Optional<String> mediaIdOpt = mediaService.uploadTemporaryMedia(amrFileOpt.get(), MediaType.VOICE);
        
        return mediaIdOpt.<Reply>map(VoiceReply::new)
                .or(() -> Optional.of(new TextReply("语音文件上传失败，无法发送给您。")));
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
