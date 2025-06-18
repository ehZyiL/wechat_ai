package xlike.top.kn_ai_chat.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.enums.MediaType;
import xlike.top.kn_ai_chat.reply.ImageReply;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.DrawingService;
import xlike.top.kn_ai_chat.service.MediaService;
import xlike.top.kn_ai_chat.service.UserConfigService;
import xlike.top.kn_ai_chat.utils.ImageCompressionUtil;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author xlike
 */
@Component
public class DrawingHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DrawingHandler.class);
    private static final long WECHAT_IMAGE_SIZE_LIMIT = 2 * 1024 * 1024;

    private final DrawingService drawingService;
    private final MediaService mediaService;
    private final ImageCompressionUtil imageCompressionUtil;
    private final UserConfigService userConfigService;


    public DrawingHandler(DrawingService drawingService, MediaService mediaService, ImageCompressionUtil imageCompressionUtil, UserConfigService userConfigService) {
        this.drawingService = drawingService;
        this.mediaService = mediaService;
        this.imageCompressionUtil = imageCompressionUtil;
        this.userConfigService = userConfigService;
    }

    @Override
    public boolean canHandle(String content) {
        // 判断消息是否以任意一个“绘画”关键词开头
        return userConfigService.getKeywordsForHandler("default", this.getClass().getSimpleName())
                .stream()
                .anyMatch(content::startsWith);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        List<String> keywords = userConfigService.getKeywordsForHandler("default", this.getClass().getSimpleName());

        String prompt = content;
        // 找到是哪个关键词触发的，并从消息中移除，得到真正的绘画提示词
        for (String keyword : keywords) {
            if (content.startsWith(keyword)) {
                prompt = content.substring(keyword.length()).trim();
                break;
            }
        }

        logger.info("接收到绘画指令，提示词: {}", prompt);

        File originalImageFile = drawingService.generateImage(prompt, externalUserId);
        if (originalImageFile == null) {
            return Optional.of(new TextReply("抱歉，绘画失败了，请稍后再试。"));
        }

        Optional<File> finalImageFileOpt = imageCompressionUtil.compressImageIfNecessary(originalImageFile, WECHAT_IMAGE_SIZE_LIMIT);

        if (finalImageFileOpt.isEmpty()) {
            originalImageFile.delete();
            return Optional.of(new TextReply("图片生成成功，但压缩至2MB以下失败了，无法发送给您。"));
        }
        
        File finalImageFile = finalImageFileOpt.get();

        Optional<String> mediaIdOpt = mediaService.uploadTemporaryMedia(finalImageFile, MediaType.IMAGE);

        if (!originalImageFile.equals(finalImageFile)) {
            originalImageFile.delete();
        }
        finalImageFile.delete();

        return mediaIdOpt.<Reply>map(ImageReply::new)
                .or(() -> Optional.of(new TextReply("图片上传失败，无法发送给您。")));
    }

    @Override
    public int getOrder() {
        return 3;
    }
}