package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class SiliconFlowService {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowService.class);

    @Value("${ai.siliconflow.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.image-model}")
    private String imageModel;
    
    @Value("${ai.siliconflow.tts-model}")
    private String ttsModel;
    
    @Value("${ai.siliconflow.stt-model}")
    private String sttModel;

    /*
     *【新增】注入视觉语言模型配置
     */
    @Value("${ai.siliconflow.vlm-model}")
    private String vlmModel;

    @Value("${ai.siliconflow.voice}")
    private String voice;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SiliconFlowService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<String> generateImageAndGetUrl(String prompt) {
        String url = baseUrl + "/images/generations";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", imageModel);
        requestBody.put("prompt", prompt);
        requestBody.put("image_size", "1024x1024");
        requestBody.put("batch_size", 1);
        requestBody.put("num_inference_steps", 20);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            String response = restTemplate.postForObject(url, requestEntity, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (root.has("images") && root.get("images").isArray() && !root.get("images").isEmpty()) {
                return Optional.of(root.get("images").get(0).get("url").asText());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("调用 SiliconFlow 绘画API时发生异常", e);
            return Optional.empty();
        }
    }

    public Optional<File> generateSpeech(String textInput) {
        String url = baseUrl + "/audio/speech";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ttsModel);
        requestBody.put("input", textInput);
        requestBody.put("voice", voice);
        requestBody.put("response_format", "mp3");
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        try {
            byte[] audioBytes = restTemplate.postForObject(url, requestEntity, byte[].class);
            if (audioBytes == null || audioBytes.length == 0) return Optional.empty();
            File tempFile = File.createTempFile("tts-", ".mp3");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(audioBytes);
            }
            tempFile.deleteOnExit();
            return Optional.of(tempFile);
        } catch (Exception e) {
            logger.error("调用 SiliconFlow TTS API 或写入文件时发生异常", e);
            return Optional.empty();
        }
    }

    public Optional<String> transcribeAudio(File audioFile) {
        String url = baseUrl + "/audio/transcriptions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioFile));
        body.add("model", sttModel);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        try {
            String response = restTemplate.postForObject(url, requestEntity, String.class);
            JsonNode root = objectMapper.readTree(response);
            if (root.has("text")) {
                return Optional.of(root.get("text").asText());
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("调用 SiliconFlow ASR API时发生异常", e);
            return Optional.empty();
        }
    }

    /**
     * 【新增方法】调用VLM分析图片内容
     *
     * @param imageFile 要分析的图片文件
     * @param prompt    对图片提问的文本
     * @return 包含AI分析结果的Optional，如果失败则为空
     */
    public Optional<String> analyzeImage(File imageFile, String prompt) {
        String url = baseUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            String base64Image = encodeFileToBase64(imageFile);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text", prompt);

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", Map.of("url", base64Image));
            
            List<Map<String, Object>> contentList = Arrays.asList(textPart, imagePart);
            
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", contentList);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", vlmModel);
            requestBody.put("messages", Collections.singletonList(message));
            requestBody.put("max_tokens", 512);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, requestEntity, String.class);
            logger.debug("SiliconFlow VLM API响应: {}", response);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                String resultText = root.get("choices").get(0).get("message").get("content").asText();
                return Optional.of(resultText);
            } else {
                logger.error("SiliconFlow VLM API未能解析响应: {}", response);
                return Optional.empty();
            }

        } catch (Exception e) {
            logger.error("调用 SiliconFlow VLM API 时发生异常", e);
            return Optional.empty();
        }
    }

    /**
     * 【新增辅助方法】将文件编码为Base64数据URI
     */
    private String encodeFileToBase64(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        /*
         * 暂时硬编码为jpeg，因为微信下载的图片通常是此格式。
         * 更稳妥的方式是根据文件头判断MIME类型。
         */
        return "data:image/jpeg;base64," + encodedString;
    }
}