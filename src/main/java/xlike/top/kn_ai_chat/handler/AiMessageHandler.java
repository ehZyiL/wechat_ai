package xlike.top.kn_ai_chat.handler;

import org.springframework.core.Ordered;
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
public class AiMessageHandler implements MessageHandler {

    private final AiService aiService;
    private final SemanticService semanticService;
    private final SiliconFlowService siliconFlowService;
    private final MediaService mediaService;
    private final FormatFileService formatFileService;

    public AiMessageHandler(AiService aiService, SemanticService semanticService, SiliconFlowService siliconFlowService, MediaService mediaService, FormatFileService formatFileService) {
        this.aiService = aiService;
        this.semanticService = semanticService;
        this.siliconFlowService = siliconFlowService;
        this.mediaService = mediaService;
        this.formatFileService = formatFileService;
    }

    @Override
    public boolean canHandle(String content) {
        return true;
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String textReply = aiService.getChatCompletion(history, openKfid);

        if (textReply == null || textReply.isEmpty()) {
            return Optional.of(new TextReply("抱歉，我暂时无法回答这个问题，请稍后再试。"));
        }

        if (semanticService.getBooleanJudgement(content, externalUserId)) {
            Optional<File> mp3FileOpt = siliconFlowService.generateSpeech(textReply, externalUserId);
            
            if (mp3FileOpt.isPresent()) {
                Optional<File> amrFileOpt = formatFileService.convertToAmr(mp3FileOpt.get());
                mp3FileOpt.get().delete();

                if (amrFileOpt.isPresent()) {
                    Optional<String> mediaIdOpt = mediaService.uploadTemporaryMedia(amrFileOpt.get(), MediaType.VOICE);
                    if (mediaIdOpt.isPresent()) {
                        return Optional.of(new VoiceReply(mediaIdOpt.get()));
                    }
                }
            }
        }
        
        return Optional.of(new TextReply(textReply));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}