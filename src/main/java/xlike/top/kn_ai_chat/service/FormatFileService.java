package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaObject;

import java.io.File;
import java.util.Optional;

/**
 * 文件格式转换服务
 * <p>
 * 使用 JAVE 库提供音频格式转换功能，
 * 例如，将微信的 AMR 格式转换为通用的 MP3 格式。
 *
 * @author Administrator
 */
@Service
public class FormatFileService {

    private static final Logger logger = LoggerFactory.getLogger(FormatFileService.class);

    /**
     * 将一个音频文件转换为 AMR 格式。
     *
     * @param sourceFile 原始音频文件（例如 MP3）
     * @return 转换后的 AMR 格式的临时文件。如果转换失败，则返回 Optional.empty()。
     */
    public Optional<File> convertToAmr(File sourceFile) {
        try {
            File targetFile = File.createTempFile("converted-", ".amr");
            targetFile.deleteOnExit();
            
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libopencore_amrnb");
            audio.setChannels(1);
            audio.setSamplingRate(8000);
            audio.setBitRate(12200);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setFormat("amr");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(sourceFile), targetFile, attrs);

            if (targetFile.exists()) {
                logger.info("文件 {} 成功转换为 AMR 格式: {}", sourceFile.getName(), targetFile.getAbsolutePath());
                return Optional.of(targetFile);
            } else {
                logger.error("文件 {} 转换为 AMR 失败，目标文件不存在。", sourceFile.getName());
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("文件转换为 AMR 时发生异常", e);
            return Optional.empty();
        }
    }

    /**
     * 【新增方法】将一个音频文件转换为 MP3 格式。
     *
     * @param sourceFile 原始音频文件（例如 AMR）
     * @return 转换后的 MP3 格式的临时文件。如果转换失败，则返回 Optional.empty()。
     */
    public Optional<File> convertToMp3(File sourceFile) {
        try {
            File targetFile = File.createTempFile("converted-", ".mp3");
            targetFile.deleteOnExit();
            
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(1);
            audio.setSamplingRate(16000);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(sourceFile), targetFile, attrs);

            if (targetFile.exists()) {
                logger.info("文件 {} 成功转换为 MP3 格式: {}", sourceFile.getName(), targetFile.getAbsolutePath());
                return Optional.of(targetFile);
            } else {
                logger.error("文件 {} 转换为 MP3 失败，目标文件不存在。", sourceFile.getName());
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("文件转换为 MP3 时发生异常", e);
            return Optional.empty();
        }
    }
}