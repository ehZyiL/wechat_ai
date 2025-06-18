package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语义服务
 * <p>
 * 通过调用一个特定的AI模型，对用户输入进行分析和判断。
 * 例如，判断用户的情绪、意图，或者是否满足某个条件。
 * @author Administrator
 */
@Service
public class SemanticService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticService.class);

    @Value("${ai.base-url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 这是用于指示AI进行判断的核心指令（System Prompt）。
     * 其中的JSON示例已按您的要求更新为单引号。
     */
    private static final String JUDGMENT_PROMPT = "请根据以下要求生成响应：1.深入分析用户输入的上下文、语气、语义及潜在意图，综合判断是否满足肯定条件，例如上下文是否需要使用语音回答，如果需要语音回答是true,否则是false。2.返回一个布尔值（true或false），true表示肯定判断，false表示否定或无法明确肯定。3.响应必须严格为以下JSON格式：{'result':true}或{'result':false}，无换行、注释或其他内容。4.若输入模糊、语义不清或无法判断，优先返回{'result':false}。示例：-输入：1+1=2，输出：{'result':true}-输入：你觉得我今天心情很好吗？（语气积极），输出：{'result':true}-输入：1+1=11，输出：{'result':false}-输入：明天会下雨吗？（无明确依据），输出：{'result':false}，对于一般的提问，都是false，当如果感觉到情绪悲伤,有波动的话，是true,请处理以下输入并返回结果：";


    public SemanticService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 根据用户输入，调用AI进行布尔值判断。
     *
     * @param userInput 用户的原始输入消息
     * @return 如果AI判断为肯定，返回 true，否则返回 false。
     */
    public boolean getBooleanJudgement(String userInput) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, String> assistantMessage = Map.of("role", "assistant", "content", JUDGMENT_PROMPT + "{" + userInput + "}");
        Map<String, String> userMessage = Map.of("role", "user", "content", userInput);
        List<Map<String, String>> messages = Arrays.asList(assistantMessage, userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            String responseStr = restTemplate.postForObject(apiUrl, requestEntity, String.class);
            logger.info("语义服务API响应: {}", responseStr);

            JsonNode root = objectMapper.readTree(responseStr);
            if(root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()){
                String contentStr = root.get("choices").get(0).get("message").get("content").asText();
                
                // ▼▼▼【核心修改】▼▼▼
                // 增强JSON解析逻辑，使其能够健壮地处理对象和数组两种情况。
                JsonNode contentJson = objectMapper.readTree(contentStr);

                // 情况一: content 是一个JSON数组, 例如 "[{'result':true}]"
                if (contentJson.isArray() && !contentJson.isEmpty()) {
                    JsonNode firstElement = contentJson.get(0);
                    if (firstElement.isObject() && firstElement.has("result")) {
                        return firstElement.get("result").asBoolean(false);
                    }
                }
                // 情况二: content 是一个JSON对象, 例如 "{'result':true}"
                else if (contentJson.isObject() && contentJson.has("result")) {
                    return contentJson.get("result").asBoolean(false);
                }
            }
        } catch (Exception e) {
            logger.error("调用或解析语义服务API时发生异常", e);
        }

        // 在任何异常或解析失败的情况下，都安全地返回 false
        return false;
    }
}