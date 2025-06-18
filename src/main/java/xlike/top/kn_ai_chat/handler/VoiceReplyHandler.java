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
 * @author xlike
 */
@Component
public class VoiceReplyHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(VoiceReplyHandler.class);

    private final AiService aiService;
    private final SiliconFlowService siliconFlowService;
    private final MediaService mediaService;
    private final FormatFileService formatFileService;
    private final UserConfigService userConfigService;

    public VoiceReplyHandler(AiService aiService, SiliconFlowService siliconFlowService, MediaService mediaService, FormatFileService formatFileService, UserConfigService userConfigService) {
        this.aiService = aiService;
        this.siliconFlowService = siliconFlowService;
        this.mediaService = mediaService;
        this.formatFileService = formatFileService;
        this.userConfigService = userConfigService;
    }

    @Override
    public boolean canHandle(String content) {
        // 判断消息是否包含任意一个“语音”关键词
        return userConfigService.getKeywordsForHandler("default", this.getClass().getSimpleName())
                .stream()
                .anyMatch(content::contains);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        List<String> triggerKeywords = userConfigService.getKeywordsForHandler("default", this.getClass().getSimpleName());
        
        String actualQuery = content;
        String foundKeyword = "";

        // 找到触发的关键词，并从内容中移除，得到真正的问题
        for (String keyword : triggerKeywords) {
            if (content.contains(keyword)) {
                foundKeyword = keyword;
                actualQuery = content.replace(keyword, "").trim();
                break;
            }
        }

        if (actualQuery.isEmpty()) {
            return Optional.of(new TextReply("您想让我用语音回答什么问题呢？"));
        }

        logger.info("接收到强制语音回复指令，关键词: '{}', 问题: {}", foundKeyword, actualQuery);

        // 创建一个新的历史记录副本，并将最后一个问题修正为移除关键词后的内容
        List<MessageLog> queryHistory = new java.util.ArrayList<>(history);
        if (!queryHistory.isEmpty()) {
            queryHistory.get(queryHistory.size() - 1).setContent(actualQuery);
        }

        String textReply = aiService.getChatCompletion(queryHistory, openKfid);

        if (textReply == null || textReply.isEmpty()) {
            return Optional.of(new TextReply("抱歉，我暂时无法回答这个问题。"));
        }

        Optional<File> mp3FileOpt = siliconFlowService.generateSpeech(textReply, externalUserId);
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
        
        amrFileOpt.ifPresent(File::delete);

        return mediaIdOpt.<Reply>map(VoiceReply::new)
                .or(() -> Optional.of(new TextReply("语音文件上传失败，无法发送给您。")));
    }

    @Override
    public int getOrder() {
        return 4;
    }
}