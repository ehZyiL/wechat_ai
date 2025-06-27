package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.mcp.Bot;
import xlike.top.kn_ai_chat.utils.MarkdownCleanerUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责与大语言模型进行交互的服务.
 *
 * @author xlike
 */
@Service
public class AiService {

    private static final Logger logger = LoggerFactory.getLogger(AiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserConfigService userConfigService;

    /**
     * 固定的JSON指令
     */
    private static final String JSON_STRUCTURE_PROMPT = " 你会用json回答用户的问题，回答的文本中，不要出现(描述)等特殊描述符号，和颜文字！并且json中只有一个reply_text，最好不要出现换行,例如[{\"answer\":{\"reply_text:'你好啊'}}],严格使用我的json结构。";


    public AiService(RestTemplate restTemplate, UserConfigService userConfigService) {
        this.restTemplate = restTemplate;
        this.userConfigService = userConfigService;
    }

    /**
     * 基于历史消息记录获取AI的聊天完成回复.
     *
     * @param history  历史消息列表
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
     * 基于特定的背景知识(上下文)进行增强的问答.
     *
     * @param userQuestion   用户的问题
     * @param context        提供的背景知识
     * @param externalUserId 外部用户ID
     * @param openKfid       客服ID
     * @return AI生成的、基于上下文的回复
     */
    public String getChatCompletionWithContext(String userQuestion, String context, String externalUserId, String openKfid) {
        AiConfig aiConfig = userConfigService.getAiConfig(externalUserId);
        logger.info("知识库RAG功能开启状态为 : {}",aiConfig.isRagEnabled());
        if (aiConfig.isRagEnabled()) {
            // RAG 模式
            logger.info("用户 [{}] 启用RAG模式进行知识库问答", externalUserId);
            return executeRagAssistant(userQuestion, context, aiConfig);
        } else {
            // 传统上下文模式
            logger.info("用户 [{}] 使用传统上下文模式进行知识库问答", externalUserId);
            return executeSimpleContextQA(userQuestion, context, aiConfig);
        }
    }


    /**
     * RAG模式：使用Langchain4j构建AI助手
     */
    private String executeRagAssistant(String userQuestion, String knowledgeBase, AiConfig aiConfig) {
        try {
            // 创建文档
            Document document = Document.from(knowledgeBase);

            // 创建嵌入模型 (Embedding Model)
            EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                    .baseUrl(aiConfig.getRagBaseUrl())
                    .apiKey(aiConfig.getRagApiKey())
                    .modelName(aiConfig.getRagModel())
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            // 创建并填充向量存储
            EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
            EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(500, 100))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build()
                    .ingest(document);
            // 创建内容检索器 (Content Retriever)
            ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                    .embeddingStore(embeddingStore)
                    .embeddingModel(embeddingModel)
                    // 检索最相关的5个片段
                    .maxResults(3)
                    // 最小相似度得分
                    .minScore(0.6)
                    .build();
            // 创建聊天模型
            String rawChatUrl = aiConfig.getAiBaseUrl();
            String chatBaseUrl = rawChatUrl;
            String suffixToRemove = "/chat/completions";
            if (rawChatUrl != null && rawChatUrl.endsWith(suffixToRemove)) {
                chatBaseUrl = rawChatUrl.substring(0, rawChatUrl.length() - suffixToRemove.length());
            }
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .baseUrl(chatBaseUrl)
                    .apiKey(aiConfig.getAiApiKey())
                    .modelName(aiConfig.getAiModel())
                    .temperature(0.3)
                    .timeout(Duration.ofSeconds(120))
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            // 使用AiServices将所有组件组合成一个AI助手
            Bot bot = AiServices.builder(Bot.class)
                    .chatModel(chatModel)
                    .contentRetriever(contentRetriever)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .build();

            String response = bot.chat(userQuestion);
            // RAG模式返回的是纯文本，我们需要将其包装成JSON
            return MarkdownCleanerUtil.cleanMarkdown(response);

        } catch (Exception e) {
            logger.error("RAG模式执行失败: {}, 正在使用传统上下文进行回答", e.getMessage());
            return executeSimpleContextQA(userQuestion, knowledgeBase, aiConfig);
        }
    }

    /**
     * 传统上下文模式：将知识库作为长字符串上下文
     */
    private String executeSimpleContextQA(String userQuestion, String context, AiConfig aiConfig) {
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
     *
     * @param messages 构造好的消息列表
     * @param baseUrl  API基础地址
     * @param apiKey   API密钥
     * @param model    模型名称
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
     * 从任何结构的 JsonNode 中提取第一个找到的 'reply_text' 字段的值。
     * 这个方法更加健壮，可以处理深度嵌套和数组。
     *
     * @param node Jackson 的 JsonNode 对象，可能为 null。
     * @return 找到的 'reply_text' 内容，如果未找到或发生错误则返回默认提示。
     */
    public String extractReplyText(JsonNode node) {
        if (node == null || node.isNull()) {
            logger.warn("输入的JSON节点为null，无法解析。");
            return "AI回复格式错误，无法解析。";
        }
        // 开始递归查找
        String result = findReplyTextRecursively(node);
        if (result != null) {
            return result;
        }
        // 如果递归查找后仍然是 null，说明没有找到
        logger.warn("未能在JSON节点中找到 'reply_text' 键: {}", node.toString());
        return "AI回复格式错误，无法解析。";
    }

    /**
     * 递归地在 JsonNode 中查找 'reply_text' 键。
     *
     * @param node 当前要搜索的节点。
     * @return 找到的文本值，或 null
     */
    private String findReplyTextRecursively(JsonNode node) {
        // 基本情况1: 如果当前节点是对象并且直接包含 "reply_text"
        if (node.isObject() && node.has("reply_text")) {
            JsonNode replyNode = node.get("reply_text");
            if (replyNode != null && replyNode.isTextual()) {
                return replyNode.asText();
            }
        }
        // 递归情况1: 如果当前节点是对象，则遍历其所有子节点
        if (node.isObject()) {
            for (JsonNode child : node) {
                String result = findReplyTextRecursively(child);
                if (result != null) {
                    return result;
                }
            }
        }
        // 递归情况2: 如果当前节点是数组，则遍历其所有元素
        if (node.isArray()) {
            for (JsonNode element : node) {
                String result = findReplyTextRecursively(element);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}