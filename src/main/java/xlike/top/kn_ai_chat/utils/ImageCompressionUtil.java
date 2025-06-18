package xlike.top.kn_ai_chat.utils;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * 图片压缩工具类
 * <p>
 * 使用 Thumbnailator 库来提供高效的图片压缩服务。
 * @author Administrator
 */
@Component
public class ImageCompressionUtil {

    private static final Logger logger = LoggerFactory.getLogger(ImageCompressionUtil.class);

    /**
     * 如果需要，则压缩图片以满足目标文件大小。
     *
     * @param sourceFile      原始图片文件
     * @param targetSizeBytes 目标文件大小（字节）
     * @return 返回一个指向最终文件的 Optional。
     * 如果原始文件已小于目标大小，则返回原始文件。
     * 如果压缩成功，则返回一个新的临时文件。
     * 如果压缩失败，则返回 Optional.empty()。
     */
    public Optional<File> compressImageIfNecessary(File sourceFile, long targetSizeBytes) {
        // 1. 如果原始文件已经达标，则直接返回
        if (sourceFile.length() <= targetSizeBytes) {
            logger.info("图片大小 {}KB 已小于目标 {}KB，无需压缩。", sourceFile.length() / 1024, targetSizeBytes / 1024);
            return Optional.of(sourceFile);
        }

        logger.info("图片大小 {}KB 超过目标 {}KB，开始压缩...", sourceFile.length() / 1024, targetSizeBytes / 1024);

        try {
            // 2. 创建一个用于存放压缩后图片的新临时文件
            File compressedFile = File.createTempFile("compressed-", ".jpg");
            compressedFile.deleteOnExit();

            // 3. 执行压缩
            //    - scale(1.0) 保持原始尺寸不变
            //    - outputQuality(0.85) 设置一个初始的较高质量
            //    - outputFormat("jpg") 转换为JPG格式通常能获得更好的压缩率
            //    - toFile() 保存到目标文件
            Thumbnails.of(sourceFile)
                    .scale(1.0)
                    .outputQuality(0.85)
                    .outputFormat("jpg")
                    .toFile(compressedFile);

            // 4. 循环检查，如果还不够小，则进一步降低质量
            //    这是一个简单的迭代压缩策略
            double quality = 0.80;
            while (compressedFile.length() > targetSizeBytes && quality > 0.1) {
                logger.info("当前大小 {}KB，继续压缩，质量因子: {}", compressedFile.length() / 1024, quality);
                Thumbnails.of(sourceFile)
                        .scale(1.0)
                        .outputQuality(quality)
                        .outputFormat("jpg")
                        .toFile(compressedFile);
                quality -= 0.1;
            }

            if (compressedFile.length() > targetSizeBytes) {
                logger.error("经过多次压缩，图片大小 {}KB 仍大于目标 {}KB，压缩失败。", compressedFile.length() / 1024, targetSizeBytes / 1024);
                return Optional.empty();
            }

            logger.info("图片压缩成功，最终大小: {}KB", compressedFile.length() / 1024);
            return Optional.of(compressedFile);

        } catch (IOException e) {
            logger.error("图片压缩过程中发生IO异常", e);
            return Optional.empty();
        }
    }
}
