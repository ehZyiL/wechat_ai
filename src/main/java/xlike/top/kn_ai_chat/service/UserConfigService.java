package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.domain.KeywordConfig;
import xlike.top.kn_ai_chat.domain.McpAiConfig;
import xlike.top.kn_ai_chat.repository.AiConfigRepository;
import xlike.top.kn_ai_chat.repository.KeywordConfigRepository;
import xlike.top.kn_ai_chat.repository.McpAiConfigRepository;

import javax.annotation.PostConstruct;
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
    private final McpAiConfigRepository mcpAiConfigRepository;

    // 默认AI配置
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


    @Value("${default.mcp.base-url}")
    private String defaultMcpBaseUrl;

    @Value("${default.mcp.api-key}")
    private String defaultMcpApiKey;

    @Value("${default.mcp.model}")
    private String defaultMcpModel;

    
    // 隐藏和追加的固定提示词
    private static final String JSON_STRUCTURE_PROMPT = " 你会用json回答用户的问题，回答的文本中，不要出现(描述)等特殊描述符号，和颜文字！并且json中只有一个reply_text，最好不要出现换行,例如[{\"answer\":{\"reply_text:'你好啊'}}],严格使用我的json结构，并且reply_text对应的内容不能超过2000字数。";


    // 默认关键词
    private static final Map<String, List<String>> DEFAULT_KEYWORDS_MAP = Map.of(
            "DrawingHandler", Arrays.asList("画一张", "画一个", "画张", "画个", "画只","画一只"),
            "LotteryHandler", Arrays.asList("大乐透", "双色球", "今日中奖号码", "查彩票", "开奖"),
            "MenuHandler", Arrays.asList("菜单", "功能", "帮助", "你能做什么", "help", "menu"),
            "SystemHandler_ClearHistory", Arrays.asList("清空历史对话", "清空记录", "清除历史对话","清空聊天记录","清除聊天记录"),
            "SystemHandler_QueryId", Arrays.asList("查询id", "我的id"),
            "SystemHandler_ChatStats", Arrays.asList("对话统计", "消息统计"),
            "SystemHandler_UserQuestions", Arrays.asList("我问过的问题", "历史问题", "我的提问"),
            "VoiceReplyHandler", Arrays.asList("语音回答", "语音回复", "用语音说", "讲一下"),
            "KnowledgeHandler", Arrays.asList("知识库")
    );


    @PostConstruct
    public void onApplicationStart() {
        initDefaultConfig();
        initDefaultMcpAiConfig();
    }

    /**
     * 初始化默认配置的核心方法。
     * 如果'default'配置不存在，则从YML文件创建它。
     * 这个方法是可重用的，供启动时和数据清除后调用。
     */
    public void initDefaultConfig() {
        if (!aiConfigRepository.existsById("default")) {
            logger.info("数据库中未找到'default'用户配置，正在从 application.yml 创建...");
            AiConfig defaultConfig = new AiConfig();
            defaultConfig.setExternalUserId("default");
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
            aiConfigRepository.save(defaultConfig);
            logger.info("'default'用户配置已成功创建并存入数据库。");
        } else {
            logger.info("数据库中已存在'default'用户配置，无需初始化。");
        }
    }


    /**
     * 初始化默认的 MCP AI 配置
     */
    public void initDefaultMcpAiConfig() {
        if (!mcpAiConfigRepository.existsById("default")) {
            logger.info("数据库中未找到'default'的MCP AI配置，正在从 application.yml 创建...");
            McpAiConfig defaultConfig = new McpAiConfig();
            defaultConfig.setExternalUserId("default");
            defaultConfig.setBaseUrl(defaultMcpBaseUrl);
            defaultConfig.setApiKey(defaultMcpApiKey);
            defaultConfig.setModel(defaultMcpModel);
            defaultConfig.setLastModified(LocalDateTime.now());
            mcpAiConfigRepository.save(defaultConfig);
            logger.info("'default'的MCP AI配置已成功创建。");
        } else {
            logger.info("数据库中已存在'default'的MCP AI配置。");
        }
    }

    public List<McpAiConfig> getAllMcpAiConfigs() {
        return mcpAiConfigRepository.findAll();
    }
    /**
     * 保存或更新一个MCP AI配置
     * @return 已保存的配置对象
     */
    @Transactional
    public McpAiConfig saveOrUpdateMcpAiConfig(McpAiConfig newConfig) {
        String userId = newConfig.getExternalUserId();
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("ExternalUserId 不能为空。");
        }

        Optional<McpAiConfig> existingConfigOpt = mcpAiConfigRepository.findByExternalUserId(userId);

        if (existingConfigOpt.isPresent()) {
            // --- 更新逻辑 ---
            logger.info("正在更新用户 [{}] 的MCP AI配置...", userId);
            McpAiConfig existingConfig = existingConfigOpt.get();

            existingConfig.setBaseUrl(newConfig.getBaseUrl());
            existingConfig.setModel(newConfig.getModel());

            if (StringUtils.hasText(newConfig.getApiKey())) {
                existingConfig.setApiKey(newConfig.getApiKey());
                logger.info("用户 [{}] 的API Key已更新。", userId);
            } else {
                logger.info("用户 [{}] 的API Key未提供，保持原有Key不变。", userId);
            }

            existingConfig.setLastModified(LocalDateTime.now());
            return mcpAiConfigRepository.save(existingConfig);

        } else {
            // --- 新增逻辑 ---
            logger.info("为用户 [{}] 新增MCP AI配置...", userId);

            // 【关键修复】如果新增时API Key为空，则从'default'配置继承
            if (!StringUtils.hasText(newConfig.getApiKey())) {
                logger.info("新增时未提供API Key，将从全局默认配置继承。");
                McpAiConfig defaultConfig = mcpAiConfigRepository.findByExternalUserId("default")
                        .orElseThrow(() -> new IllegalStateException("无法找到全局默认配置来继承API Key。"));
                newConfig.setApiKey(defaultConfig.getApiKey());
            }

            newConfig.setLastModified(LocalDateTime.now());
            return mcpAiConfigRepository.save(newConfig);
        }
    }

    /**
     * 【新增】重置（删除）一个用户的MCP AI配置
     * @param externalUserId 要重置配置的用户ID
     */
    public void resetMcpAiConfig(String externalUserId) {
        if ("default".equals(externalUserId)) {
            logger.warn("不允许删除'default'的MCP AI配置。");
            return;
        }
        mcpAiConfigRepository.findByExternalUserId(externalUserId).ifPresent(config -> {
            logger.info("正在删除用户 [{}] 的MCP AI配置，使其回退到default。", externalUserId);
            mcpAiConfigRepository.delete(config);
        });
    }

    /**
     * 获取 MCP 的 AI 配置，支持用户级和全局默认
     * @param externalUserId 外部用户ID
     * @return 最终生效的McpAiConfig对象
     */
    public McpAiConfig getMcpAiConfig(String externalUserId) {
        // 1. 尝试获取用户专属配置
        Optional<McpAiConfig> userConfig = mcpAiConfigRepository.findByExternalUserId(externalUserId);
        if (userConfig.isPresent()) {
            logger.debug("为用户 [{}] 找到并使用其个人MCP AI配置。", externalUserId);
            return userConfig.get();
        }
        // 2. 如果没有，则获取并返回全局'default'配置
        logger.debug("用户 [{}] 无个人MCP AI配置，使用全局'default'配置。", externalUserId);
        return mcpAiConfigRepository.findByExternalUserId("default")
                .orElseThrow(() -> new IllegalStateException("数据库中未找到'default'的MCP AI配置，请检查初始化过程。"));
    }


    public UserConfigService(AiConfigRepository aiConfigRepository,
                             KeywordConfigRepository keywordConfigRepository,
                             McpAiConfigRepository mcpAiConfigRepository) {
        this.aiConfigRepository = aiConfigRepository;
        this.keywordConfigRepository = keywordConfigRepository;
        this.mcpAiConfigRepository = mcpAiConfigRepository;
    }

    public AiConfig getAiConfig(String externalUserId) {
        // 优先级1：查找指定用户的个人配置
        Optional<AiConfig> userConfig = aiConfigRepository.findByExternalUserId(externalUserId);
        if (userConfig.isPresent()) {
            logger.info("为用户 {} 找到并使用其个人AI配置。", externalUserId);
            return processPrompt(userConfig.get());
        }
        logger.debug("用户 {} 无个人配置，尝试查找全局'default'配置...", externalUserId);
        Optional<AiConfig> dbDefaultConfig = aiConfigRepository.findByExternalUserId("default");
        if (dbDefaultConfig.isPresent()) {
            return createConfigForUser(externalUserId, dbDefaultConfig.get());
        }
        // 优先级3：如果数据库连'default'配置都没有，则使用application.yml中的最终后备值
        logger.warn("数据库中未找到全局'default'配置，为用户 {} 应用YML文件中的最终后备配置。", externalUserId);
        return createFallbackConfig(externalUserId);
    }

    // 处理Prompt，移除内部指令
    private AiConfig processPrompt(AiConfig config) {
        if (config.getSystemPrompt() != null && config.getSystemPrompt().contains(JSON_STRUCTURE_PROMPT)) {
            config.setSystemPrompt(config.getSystemPrompt().replace(JSON_STRUCTURE_PROMPT, ""));
        }
        return config;
    }

    // 根据一个模板配置为指定用户创建一个新配置对象
    private AiConfig createConfigForUser(String externalUserId, AiConfig templateConfig) {
        AiConfig newConfig = new AiConfig();
        newConfig.setExternalUserId(externalUserId);
        newConfig.setAiBaseUrl(templateConfig.getAiBaseUrl());
        newConfig.setAiApiKey(templateConfig.getAiApiKey());
        newConfig.setAiModel(templateConfig.getAiModel());
        newConfig.setSystemPrompt(templateConfig.getSystemPrompt());
        newConfig.setSfBaseUrl(templateConfig.getSfBaseUrl());
        newConfig.setSfImageModel(templateConfig.getSfImageModel());
        newConfig.setSfTtsModel(templateConfig.getSfTtsModel());
        newConfig.setSfSttModel(templateConfig.getSfSttModel());
        newConfig.setSfVoice(templateConfig.getSfVoice());
        newConfig.setSfVlmModel(templateConfig.getSfVlmModel());
        newConfig.setLastModified(templateConfig.getLastModified());
        return processPrompt(newConfig);
    }

    // 创建最终的后备配置
    private AiConfig createFallbackConfig(String externalUserId) {
        AiConfig fallbackConfig = new AiConfig();
        fallbackConfig.setExternalUserId(externalUserId);
        fallbackConfig.setAiBaseUrl(defaultAiBaseUrl);
        fallbackConfig.setAiApiKey(defaultAiApiKey);
        fallbackConfig.setAiModel(defaultAiModel);
        fallbackConfig.setSystemPrompt(defaultSystemPrompt);
        fallbackConfig.setSfBaseUrl(defaultSfBaseUrl);
        fallbackConfig.setSfImageModel(defaultSfImageModel);
        fallbackConfig.setSfTtsModel(defaultSfTtsModel);
        fallbackConfig.setSfSttModel(defaultSfSttModel);
        fallbackConfig.setSfVoice(defaultSfVoice);
        fallbackConfig.setSfVlmModel(defaultSfVlmModel);
        fallbackConfig.setLastModified(LocalDateTime.now());
        return processPrompt(fallbackConfig);
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