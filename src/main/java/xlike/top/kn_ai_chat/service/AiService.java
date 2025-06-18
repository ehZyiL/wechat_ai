package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.domain.MessageLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责与大语言模型进行交互的服务.
 * @author Administrator
 */
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserConfigService userConfigService;

    /**
     * 【修改】将固定的JSON指令定义为常量，以便复用.
     */
    private static final String JSON_STRUCTURE_PROMPT = " 你会用json回答用户的问题，回答的文本中，不要出现(描述)等特殊描述符号，和颜文字！并且json中只有一个reply_text，最好不要出现换行,例如[{\"answer\":{\"reply_text:'你好啊'}}],严格使用我的json结构。";


    public AiService(RestTemplate restTemplate, UserConfigService userConfigService) {
        this.restTemplate = restTemplate;
        this.userConfigService = userConfigService;
    }

    /**
     * 基于历史消息记录获取AI的聊天完成回复.
     * @param history 历史消息列表
     * @param openKfid 客服ID
     * @return AI生成的回复文本
     */
    public String getChatCompletion(List<MessageLog> history, String openKfid) {
        AiConfig aiConfig = userConfigService.getAiConfig(history.get(0).getFromUser());

        List<Map<String, String>> messages = new ArrayList<>();
        String userSystemPrompt = aiConfig.getSystemPrompt();
        String finalSystemPrompt;

        // 检查用户配置是否已包含指令
        if (userSystemPrompt != null && userSystemPrompt.contains(JSON_STRUCTURE_PROMPT)) {
            finalSystemPrompt = userSystemPrompt;
        } else {
            //如果不包含，则将用户配置和我们的指令拼接起来
            finalSystemPrompt = userSystemPrompt + JSON_STRUCTURE_PROMPT;
        }
        
        messages.add(Map.of("role", "system", "content", finalSystemPrompt));

        for (MessageLog log : history) {
            Map<String, String> message = new HashMap<>();
            if (log.getFromUser().equals(openKfid)) {
                message.put("role", "assistant");
            } else {
                message.put("role", "user");
            }
            message.put("content", log.getContent());
            messages.add(message);
        }
        
        return executeChatCompletion(messages, aiConfig.getAiBaseUrl(), aiConfig.getAiApiKey(), aiConfig.getAiModel());
    }

    /**
     * 基于特定的背景知识(上下文)进行增强的问答(RAG).
     * @param userQuestion 用户的问题
     * @param context 提供的背景知识
     * @param externalUserId 外部用户ID
     * @param openKfid 客服ID
     * @return AI生成的、基于上下文的回复
     */
    public String getChatCompletionWithContext(String userQuestion, String context, String externalUserId, String openKfid) {
        AiConfig aiConfig = userConfigService.getAiConfig(externalUserId);

        String ragSystemPrompt = String.format(
                "你是一个智能助手，请严格根据下面提供的“背景知识”来回答用户的问题。" +
                "如果背景知识中没有相关信息，就在返回的JSON中说明情况。" +
                "你的回答必须严格遵循JSON格式：[{\"answer\":{\"reply_text\":\"您的回答\"}}]，不要包含任何其他多余的文字或解释。\n\n" +
                "--- 背景知识开始 ---\n%s\n--- 背景知识结束 ---",
                context
        );

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", ragSystemPrompt));
        messages.add(Map.of("role", "user", "content", userQuestion));

        return executeChatCompletion(messages, aiConfig.getAiBaseUrl(), aiConfig.getAiApiKey(), aiConfig.getAiModel());
    }
    
    /**
     * 执行对大模型API的调用.
     * @param messages 构造好的消息列表
     * @param baseUrl API基础地址
     * @param apiKey API密钥
     * @param model 模型名称
     * @return AI返回的原始JSON字符串中的内容部分
     */
    private String executeChatCompletion(List<Map<String, String>> messages, String baseUrl, String apiKey, String model) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(baseUrl, requestEntity, String.class);
            logger.info("AI模型响应: {}", response);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String contentJsonString = firstChoice.get("message").get("content").asText();
                    
                    logger.info("准备解析AI内容JSON: {}", contentJsonString);
                    JsonNode contentRoot = objectMapper.readTree(contentJsonString);
                    
                    if (contentRoot.isObject()) {
                        return extractReplyText(contentRoot);
                    } else if (contentRoot.isArray() && !contentRoot.isEmpty()) {
                        return extractReplyText(contentRoot.get(0));
                    }
                    return "AI返回了无效的JSON格式。";
                }
            }
        } catch (Exception e) {
            logger.error("调用AI模型接口或解析JSON失败", e);
        }

        return "抱歉，AI服务当前不可用，请稍后再试。";
    }

    /**
     * 从AI返回的JSON内容中提取最终的回复文本.
     * @param node JSON节点
     * @return 回复文本
     */
    private String extractReplyText(JsonNode node) {
        if (node != null && node.isObject() && node.has("answer") && node.get("answer").has("reply_text")) {
            return node.get("answer").get("reply_text").asText();
        }
        logger.warn("未能从JSON节点中找到 'answer.reply_text' 路径: {}", node != null ? node.toString() : "null");
        return "AI回复格式错误，无法解析。";
    }
}