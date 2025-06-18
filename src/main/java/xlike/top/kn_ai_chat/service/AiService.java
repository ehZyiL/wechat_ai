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
import xlike.top.kn_ai_chat.domain.MessageLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    @Value("${ai.base-url}")
    private String aiBaseUrl;
    @Value("${ai.api-key}")
    private String aiApiKey;
    @Value("${ai.model}")
    private String aiModel;
    @Value("${ai.system-prompt}")
    private String systemPrompt;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用AI大模型进行常规对话
     *
     * @param history  历史消息
     * @param openKfid 客服ID
     * @return AI回复的纯文本
     */
    public String getChatCompletion(List<MessageLog> history, String openKfid) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));

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
        
        return executeChatCompletion(messages);
    }

    /**
     * 基于提供的上下文进行问答 (RAG)
     *
     * @param userQuestion 用户的提问
     * @param context      从知识库中检索到的上下文信息
     * @param openKfid     客服ID
     * @return AI的回答
     */
    public String getChatCompletionWithContext(String userQuestion, String context, String openKfid) {
        /*
         * 【核心修改】更新RAG的System Prompt，明确指示AI以JSON格式返回答案。
         * 这里的Prompt结合了RAG的要求和您对JSON格式的严格要求。
         */
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

        return executeChatCompletion(messages);
    }
    
    /**
     * 执行Chat Completion请求的私有通用方法
     *
     * @param messages 构造好的消息列表
     * @return AI回复的文本，或在出错时返回默认错误信息
     */
    private String executeChatCompletion(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + aiApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiModel);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        
        if (true) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            String response = restTemplate.postForObject(aiBaseUrl, requestEntity, String.class);
            logger.info("AI模型响应: {}", response);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    String contentJsonString = firstChoice.get("message").get("content").asText();
                    
                    /*
                     * 由于现在所有场景都要求返回JSON，我们统一使用JSON解析逻辑
                     */
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
     * 从一个JSON对象中，按 "answer.reply_text" 路径提取文本
     *
     * @param node 要解析的JSON节点 (应为一个ObjectNode)
     * @return 提取到的文本，或在找不到时返回null
     */
    private String extractReplyText(JsonNode node) {
        if (node != null && node.isObject() && node.has("answer") && node.get("answer").has("reply_text")) {
            return node.get("answer").get("reply_text").asText();
        }
        logger.warn("未能从JSON节点中找到 'answer.reply_text' 路径: {}", node != null ? node.toString() : "null");
        return "AI回复格式错误，无法解析。";
    }
}