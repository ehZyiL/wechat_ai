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
import xlike.top.kn_ai_chat.utils.ImageCompressionUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
@Component
public class DrawingHandler implements MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(DrawingHandler.class);
    private static final List<String> KEYWORDS = Arrays.asList("画一张", "画一个", "画张", "画个");
    // 定义微信图片上传大小限制 (2MB)
    private static final long WECHAT_IMAGE_SIZE_LIMIT = 2 * 1024 * 1024;

    private final DrawingService drawingService;
    private final MediaService mediaService;
    private final ImageCompressionUtil imageCompressionUtil;

    public DrawingHandler(DrawingService drawingService, MediaService mediaService, ImageCompressionUtil imageCompressionUtil) {
        this.drawingService = drawingService;
        this.mediaService = mediaService;
        this.imageCompressionUtil = imageCompressionUtil;
    }

    @Override
    public boolean canHandle(String content) {
        return KEYWORDS.stream().anyMatch(content::startsWith);
    }

    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        String prompt = content;
        for (String keyword : KEYWORDS) {
            if (content.startsWith(keyword)) {
                prompt = content.substring(keyword.length()).trim();
                break;
            }
        }
        logger.info("接收到绘画指令，提示词: {}", prompt);

        // 1. 从AI生成原始图片
        File originalImageFile = drawingService.generateImage(prompt);
        if (originalImageFile == null) {
            return Optional.of(new TextReply("抱歉，绘画失败了，真的是FW木池，请稍后再试。"));
        }

        // 2. 调用工具类进行压缩
        Optional<File> finalImageFileOpt = imageCompressionUtil.compressImageIfNecessary(originalImageFile, WECHAT_IMAGE_SIZE_LIMIT);

        // 【重要修正】如果压缩失败，清理原始文件并返回错误
        if (finalImageFileOpt.isEmpty()) {
            originalImageFile.delete(); // 清理原始文件
            return Optional.of(new TextReply("图片生成成功，但压缩至2MB以下失败了，无法发送给您。"));
        }
        
        File finalImageFile = finalImageFileOpt.get();

        // 3. 上传最终处理过的图片
        Optional<String> mediaIdOpt = mediaService.uploadTemporaryMedia(finalImageFile, MediaType.IMAGE);

        // 4. 【重要修正】在上传完成后，再进行文件清理
        // 如果压缩过程创建了一个新文件（即 finalImageFile 和 originalImageFile 不是同一个文件），
        // 那么原始文件现在可以安全地删除了。
        if (!originalImageFile.equals(finalImageFile)) {
            originalImageFile.delete();
        }
        // 无论如何，最终被上传的那个文件（可能是原始文件，也可能是压缩后的文件）在使用完毕后都应删除。
        finalImageFile.delete();

        // 5. 返回结果
        return mediaIdOpt.<Reply>map(ImageReply::new)
                .or(() -> Optional.of(new TextReply("图片上传失败，无法发送给您。")));
    }

    @Override
    public int getOrder() {
        return 3;
    }
}