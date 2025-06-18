package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Optional;

@Service
public class DrawingService {

    private static final Logger logger = LoggerFactory.getLogger(DrawingService.class);

    private final SiliconFlowService siliconFlowService;
    private final RestTemplate restTemplate;

    public DrawingService(SiliconFlowService siliconFlowService, RestTemplate restTemplate) {
        this.siliconFlowService = siliconFlowService;
        this.restTemplate = restTemplate;
    }

    public File generateImage(String prompt, String externalUserId) {
        Optional<String> imageUrlOpt = siliconFlowService.generateImageAndGetUrl(prompt, externalUserId);

        if (imageUrlOpt.isEmpty()) {
            logger.error("未能获取到图片URL，绘画失败。");
            return null;
        }

        String imageUrl = imageUrlOpt.get();

        try {
            URI uri = new URI(imageUrl);
            logger.info("准备从URI下载图片: {}", uri);

            byte[] imageBytes = restTemplate.getForObject(uri, byte[].class);
            
            if (imageBytes == null || imageBytes.length == 0) {
                logger.error("从URI下载的图片数据为空。URI: {}", uri);
                return null;
            }
            
            File tempFile = File.createTempFile("sf-drawing-", ".png");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(imageBytes);
            }
            
            tempFile.deleteOnExit();

            logger.info("图片下载成功并存为临时文件: {}", tempFile.getAbsolutePath());
            return tempFile;

        } catch (Exception e) {
            logger.error("从URL下载或保存图片时发生错误, URL: {}", imageUrl, e);
            return null;
        }
    }
}