package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.domain.KeywordConfig;
import xlike.top.kn_ai_chat.repository.AiConfigRepository;
import xlike.top.kn_ai_chat.repository.KeywordConfigRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author xlike
 */
@Service
public class UserConfigService {

    private static final Logger logger = LoggerFactory.getLogger(UserConfigService.class);

    private final AiConfigRepository aiConfigRepository;
    private final KeywordConfigRepository keywordConfigRepository;

    // Default AI Config values
    @Value("${default.ai.base-url}")
    private String defaultAiBaseUrl;
    @Value("${default.ai.api-key}")
    private String defaultAiApiKey;
    @Value("${default.ai.model}")
    private String defaultAiModel;
    @Value("${default.ai.system-prompt}")
    private String defaultSystemPrompt;
    @Value("${default.ai.siliconflow.base-url}")
    private String defaultSfBaseUrl;
    @Value("${default.ai.siliconflow.image-model}")
    private String defaultSfImageModel;
    @Value("${default.ai.siliconflow.tts-model}")
    private String defaultSfTtsModel;
    @Value("${default.ai.siliconflow.stt-model}")
    private String defaultSfSttModel;
    @Value("${default.ai.siliconflow.voice}")
    private String defaultSfVoice;
    @Value("${default.ai.siliconflow.vlm-model}")
    private String defaultSfVlmModel;
    
    // 隐藏和追加的固定提示词
    private static final String JSON_STRUCTURE_PROMPT = " 你会用json回答用户的问题，回答的文本中，不要出现(描述)等特殊描述符号，和颜文字！并且json中只有一个reply_text，最好不要出现换行,例如[{\"answer\":{\"reply_text:'你好啊'}}],严格使用我的json结构，并且reply_text对应的内容不能超过2000字数。";


    // Default Keywords. Using more specific names for SystemHandler's sub-commands
    private static final Map<String, List<String>> DEFAULT_KEYWORDS_MAP = Map.of(
            "DrawingHandler", Arrays.asList("画一张", "画一个", "画张", "画个"),
            "LotteryHandler", Arrays.asList("大乐透", "双色球", "今日中奖号码", "查彩票", "开奖"),
            "MenuHandler", Arrays.asList("菜单", "功能", "帮助", "你能做什么", "help", "menu"),
            // SystemHandler sub-commands
            "SystemHandler_ClearHistory", Arrays.asList("清空历史对话", "清空记录", "清除历史对话","清空聊天记录"),
            "SystemHandler_QueryId", Arrays.asList("查询id", "我的id"),
            "SystemHandler_ChatStats", Arrays.asList("对话统计", "消息统计"),
            "SystemHandler_UserQuestions", Arrays.asList("我问过的问题", "历史问题", "我的提问"),
            "VoiceReplyHandler", Arrays.asList("语音回答", "语音回复", "用语音说", "讲一下"),
            "KnowledgeHandler", Arrays.asList("知识库")
    );


    public UserConfigService(AiConfigRepository aiConfigRepository, KeywordConfigRepository keywordConfigRepository) {
        this.aiConfigRepository = aiConfigRepository;
        this.keywordConfigRepository = keywordConfigRepository;
    }

    public AiConfig getAiConfig(String externalUserId) {
        // 1. 获取配置，如果不存在则使用默认配置
        AiConfig config = aiConfigRepository.findByExternalUserId(externalUserId).orElseGet(() -> {
            logger.info("用户 {} 无自定义AI配置，使用默认配置。", externalUserId);
            AiConfig defaultConfig = new AiConfig();
            defaultConfig.setExternalUserId(externalUserId);
            defaultConfig.setAiBaseUrl(defaultAiBaseUrl);
            defaultConfig.setAiApiKey(defaultAiApiKey);
            defaultConfig.setAiModel(defaultAiModel);
            defaultConfig.setSystemPrompt(defaultSystemPrompt);
            defaultConfig.setSfBaseUrl(defaultSfBaseUrl);
            defaultConfig.setSfImageModel(defaultSfImageModel);
            defaultConfig.setSfTtsModel(defaultSfTtsModel);
            defaultConfig.setSfSttModel(defaultSfSttModel);
            defaultConfig.setSfVoice(defaultSfVoice);
            defaultConfig.setSfVlmModel(defaultSfVlmModel);
            defaultConfig.setLastModified(LocalDateTime.now());
            return defaultConfig;
        });

        // 2. 过滤掉从数据库中可能读出的、包含内部指令的旧数据
        if (config.getSystemPrompt() != null && config.getSystemPrompt().contains(JSON_STRUCTURE_PROMPT)) {
            config.setSystemPrompt(config.getSystemPrompt().replace(JSON_STRUCTURE_PROMPT, ""));
        }

        // 3. 返回处理过的配置对象
        return config;
    }

    public AiConfig saveOrUpdateAiConfig(AiConfig aiConfig) {
        aiConfig.setLastModified(LocalDateTime.now());
        logger.info("保存或更新用户 {} 的AI配置。", aiConfig.getExternalUserId());
        return aiConfigRepository.save(aiConfig);
    }

    public List<String> getKeywordsForHandler(String externalUserId, String handlerName) {
        Optional<KeywordConfig> config = keywordConfigRepository.findByExternalUserIdAndHandlerName(externalUserId, handlerName);
        if (config.isPresent()) {
            return Arrays.asList(config.get().getKeywords().split(","));
        } else {
            List<String> defaultKeywords = DEFAULT_KEYWORDS_MAP.getOrDefault(handlerName, List.of());
            logger.debug("用户 {} 无自定义 {} 关键词，使用默认关键词：{}", externalUserId, handlerName, defaultKeywords);
            return defaultKeywords;
        }
    }

    public Map<String, List<String>> getAllKeywords(String externalUserId) {
        List<KeywordConfig> userKeywordConfigs = keywordConfigRepository.findByExternalUserId(externalUserId);
        Map<String, List<String>> userKeywordsMap = userKeywordConfigs.stream()
                .collect(Collectors.toMap(KeywordConfig::getHandlerName, kc -> Arrays.asList(kc.getKeywords().split(","))));

        DEFAULT_KEYWORDS_MAP.forEach(userKeywordsMap::putIfAbsent);
        return userKeywordsMap;
    }

    public KeywordConfig saveOrUpdateKeywordConfig(String externalUserId, String handlerName, List<String> keywords) {
        KeywordConfig config = keywordConfigRepository.findByExternalUserIdAndHandlerName(externalUserId, handlerName)
                .orElse(new KeywordConfig());
        config.setExternalUserId(externalUserId);
        config.setHandlerName(handlerName);
        config.setKeywords(String.join(",", keywords));
        config.setLastModified(LocalDateTime.now());
        logger.info("保存或更新用户 {} 的 {} 关键词配置。", externalUserId, handlerName);
        return keywordConfigRepository.save(config);
    }
}