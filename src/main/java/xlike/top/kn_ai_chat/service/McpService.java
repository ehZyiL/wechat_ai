package xlike.top.kn_ai_chat.service;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.McpAiConfig;
import xlike.top.kn_ai_chat.domain.McpConfig;
import xlike.top.kn_ai_chat.domain.UserMcpPermission;
import xlike.top.kn_ai_chat.domain.WeChatUser;
import xlike.top.kn_ai_chat.dto.McpAddRequest;
import xlike.top.kn_ai_chat.dto.McpDefinition;
import xlike.top.kn_ai_chat.mcp.Bot;
import xlike.top.kn_ai_chat.repository.McpConfigRepository;
import xlike.top.kn_ai_chat.repository.UserMcpPermissionRepository;
import xlike.top.kn_ai_chat.repository.WeChatUserRepository;
import xlike.top.kn_ai_chat.utils.MarkdownCleanerUtil;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * McpService 已重构为使用 Langchain4j，并移除了旧的 Spring AI 启动加载逻辑。
 * @author xlike
 */
@Service
public class McpService {

    private static final Logger logger = LoggerFactory.getLogger(McpService.class);
    private static final String MCP_CACHE_KEY_PREFIX = "kn_ai_chat:mcp:list:";
    private static final long CACHE_TIMEOUT_MINUTES = 20;

    private final McpConfigRepository mcpConfigRepository;
    private final UserMcpPermissionRepository permissionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WeChatUserRepository weChatUserRepository;
    private final UserConfigService userConfigService;

    public McpService(McpConfigRepository mcpConfigRepository,
                      UserMcpPermissionRepository permissionRepository,
                      RedisTemplate<String, Object> redisTemplate,
                      WeChatUserRepository weChatUserRepository,
                      UserConfigService userConfigService) {
        this.mcpConfigRepository = mcpConfigRepository;
        this.permissionRepository = permissionRepository;
        this.redisTemplate = redisTemplate;
        this.weChatUserRepository = weChatUserRepository;
        this.userConfigService = userConfigService;
    }

    /**
     * 使用 Langchain4j 执行 MCP 请求。
     */
    @Transactional(readOnly = true)
    public String executeMcpRequest(String externalUserId, String mcpConnectionName, String prompt) {
        if (!hasPermission(externalUserId, mcpConnectionName)) {
            logger.warn("用户 [{}] 尝试访问未授权的 MCP 连接 [{}]", externalUserId, mcpConnectionName);
            return "❌ 您没有权限使用名为 '" + mcpConnectionName + "' 的模型服务。请联系管理员授权。";
        }

        McpConfig mcpConfig = mcpConfigRepository.findByName(mcpConnectionName)
                .orElseThrow(() -> new IllegalArgumentException("名为 '" + mcpConnectionName + "' 的 MCP 配置不存在。"));

        // 动态获取当前用户的MCP AI配置
        McpAiConfig mcpAiConfig = userConfigService.getMcpAiConfig(externalUserId);
        logger.info("为用户 [{}] 加载了MCP AI配置: {}", externalUserId, mcpAiConfig.getModel());

        McpClient mcpClient = null;
        try {
            logger.info("用户 [{}] 通过 MCP 连接 [{}] 发起请求...", externalUserId, mcpConnectionName);

            // 使用从UserConfigService获取的动态配置来构建ChatModel
            ChatModel model = OpenAiChatModel.builder()
                    .apiKey(mcpAiConfig.getApiKey())
                    .baseUrl(mcpAiConfig.getBaseUrl())
                    .modelName(mcpAiConfig.getModel())
                    .timeout(Duration.ofSeconds(120))
                    .maxRetries(3)
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl(mcpConfig.getSseEndpoint())
                    .timeout(Duration.ofSeconds(120))
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            mcpClient = new DefaultMcpClient.Builder().transport(transport).build();
            ToolProvider toolProvider = McpToolProvider.builder().mcpClients(List.of(mcpClient)).build();

            Bot bot = AiServices.builder(Bot.class)
                    .chatModel(model)
                    .toolProvider(toolProvider)
                    .build();

            String finalPrompt = "你在回答的时候，不能出现markdown格式的任何语法，例如**,#,```等等，任何的md语法的符号都不能输出。" + prompt;
            String response = bot.chat(finalPrompt);

            return MarkdownCleanerUtil.cleanMarkdown(response);
        } catch (Exception e) {
            logger.error("调用 MCP 连接 [{}] 时发生未知错误，用户: {}。错误: {}", mcpConnectionName, externalUserId, e.getMessage(), e);
            return "调用模型服务 '" + mcpConnectionName + "' 时发生内部错误: " + e.getClass().getSimpleName();
        } finally {
            if (mcpClient != null) {
                try {
                    mcpClient.close();
                } catch (Exception e) {
                    logger.error("关闭 MCP Client 时出错", e);
                }
            }
        }
    }

    /**
     * 根据传入的JSON对象新增MCP配置，并保存type字段
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMcpConfig(McpAddRequest request) {
        if (request.getMcpServers() == null || request.getMcpServers().isEmpty()) {
            throw new IllegalArgumentException("请求中必须包含 mcpServers 信息。");
        }

        for (Map.Entry<String, McpDefinition> entry : request.getMcpServers().entrySet()) {
            String name = entry.getKey();
            McpDefinition definition = entry.getValue();
            String url = definition.getUrl();
            String type = definition.getType();

            logger.info("正在处理新的MCP配置: [{}], 类型: [{}], URL: {}", name, type, url);

            if (mcpConfigRepository.findByName(name).isPresent()) {
                logger.warn("MCP配置 [{}] 已存在，跳过新增。", name);
                throw new IllegalArgumentException("配置名称 '" + name + "' 已存在，请使用其他名称。");
            }

            if (!isMcpEndpointValid(url)) {
                logger.error("URL连接测试失败: {}", url);
                throw new RuntimeException("无法连接到URL: '" + url + "'，请检查网络或地址是否正确。");
            }

            McpConfig newConfig = new McpConfig();
            newConfig.setName(name);
            newConfig.setType(type);
            newConfig.setUrl(url);
            newConfig.setSseEndpoint(url);
            mcpConfigRepository.save(newConfig);

            logger.info("成功新增MCP配置: [{}], 类型: [{}], SSE Endpoint: {}", name, type, url);
        }
    }

    /**
     *  删除一个MCP配置及其所有相关的权限记录
     * @param mcpConfigId 要删除的MCP配置的ID
     */
    @Transactional(rollbackFor = Exception.class) // 保证事务性
    public void deleteMcpConfig(Long mcpConfigId) {
        if (!mcpConfigRepository.existsById(mcpConfigId)) {
            logger.warn("尝试删除不存在的MCP配置，ID: {}", mcpConfigId);
            throw new IllegalArgumentException("ID为 " + mcpConfigId + " 的MCP配置不存在。");
        }

        logger.info("正在删除 MCP 配置 ID: {}", mcpConfigId);
        // 先删除所有与此MCP配置相关的权限记录，避免违反外键约束
        long deletedPermissionsCount = permissionRepository.deleteByMcpConfigId(mcpConfigId);
        logger.info("删除了 {} 条与 MCP 配置 ID [{}] 相关的权限记录。", deletedPermissionsCount, mcpConfigId);
        // 再删除MCP配置本身
        mcpConfigRepository.deleteById(mcpConfigId);
        logger.info("成功删除 MCP 配置 ID: {}", mcpConfigId);
    }

    /**
     * 测试给定的URL是否可以访问
     */
    private boolean isMcpEndpointValid(String sseUrl) {
        McpClient mcpClient = null;
        try {
            logger.info("正在验证MCP端点的有效性: {}", sseUrl);
            // 构建Transport 和 Client
            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl(sseUrl)
                    .timeout(Duration.ofSeconds(30))
                    .build();

            mcpClient = new DefaultMcpClient.Builder()
                    .transport(transport)
                    .build();
            // 列出工具
            List<ToolSpecification> tools = mcpClient.listTools();
            if (tools != null && !tools.isEmpty()) {
                logger.info("MCP端点 [{}] 验证成功，获取到 {} 个工具。", sseUrl, tools.size());
                return true;
            } else {
                logger.warn("MCP端点 [{}] 连接成功，但未返回任何工具。", sseUrl);
                return false;
            }
        } catch (Exception e) {
            // 任何异常（连接超时、握手失败、解析错误等）都意味着端点无效
            logger.error("验证MCP端点 [{}] 时发生异常: {}", sseUrl, e.getMessage());
            return false;
        } finally {
            // 无论成功与否都关闭客户端以释放资源
            if (mcpClient != null) {
                try {
                    mcpClient.close();
                } catch (Exception e) {
                    logger.error("关闭用于验证的 MCP Client 时出错", e);
                }
            }
        }
    }
    

    @Transactional
    public void batchUpdatePermissions(Long mcpConfigId, boolean grant) {
        McpConfig config = mcpConfigRepository.findById(mcpConfigId)
                .orElseThrow(() -> new IllegalArgumentException("无效的 MCP 配置 ID: " + mcpConfigId));

        List<WeChatUser> allUsers = weChatUserRepository.findAll();
        logger.info("开始为 MCP [{}] 批量 {} {} 个用户的权限。", config.getName(), grant ? "授予" : "撤销", allUsers.size());

        if (grant) {
            Set<String> userIdsWithPermission = permissionRepository.findByMcpConfigId(mcpConfigId).stream()
                    .map(UserMcpPermission::getExternalUserId)
                    .collect(Collectors.toSet());

            List<UserMcpPermission> permissionsToSave = allUsers.stream()
                    .filter(user -> !userIdsWithPermission.contains(user.getExternalUserId()))
                    .map(user -> {
                        UserMcpPermission permission = new UserMcpPermission();
                        permission.setExternalUserId(user.getExternalUserId());
                        permission.setMcpConfig(config);
                        return permission;
                    })
                    .collect(Collectors.toList());

            if (!permissionsToSave.isEmpty()) {
                permissionRepository.saveAll(permissionsToSave);
                logger.info("为 {} 个新用户授予了 MCP [{}] 的权限。", permissionsToSave.size(), config.getName());
            }
        } else {
            long deletedCount = permissionRepository.deleteByMcpConfigId(mcpConfigId);
            logger.info("为 MCP [{}] 撤销了 {} 条权限记录。", config.getName(), deletedCount);
        }
    }
    
    @Transactional(readOnly = true)
    public List<McpConfig> getAndCacheAuthorizedMcpListForUser(String externalUserId) {
        List<UserMcpPermission> permissions = permissionRepository.findByExternalUserId(externalUserId);
        
        List<McpConfig> authorizedConfigs = permissions.stream()
                .map(permission -> {
                    McpConfig config = permission.getMcpConfig();
                    McpConfig cleanConfig = new McpConfig();
                    cleanConfig.setId(config.getId());
                    cleanConfig.setName(config.getName());
                    cleanConfig.setType(config.getType()); // 也将type返回给前端
                    cleanConfig.setUrl(config.getUrl());
                    cleanConfig.setSseEndpoint(config.getSseEndpoint());
                    return cleanConfig;
                })
                .collect(Collectors.toList());
        
        if (!authorizedConfigs.isEmpty()) {
            try {
                String key = MCP_CACHE_KEY_PREFIX + externalUserId;
                redisTemplate.opsForValue().set(key, authorizedConfigs, CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                logger.info("为用户 [{}] 在 Redis 中缓存了 {} 个可用的 MCP 服务。", externalUserId, authorizedConfigs.size());
            } catch (Exception e) {
                logger.error("将 MCP 列表缓存到 Redis 时失败，用户: {}", externalUserId, e);
            }
        }
        
        return authorizedConfigs;
    }
    
    @SuppressWarnings("unchecked")
    public Optional<McpConfig> getMcpConfigFromCache(String externalUserId, int index) {
        String key = MCP_CACHE_KEY_PREFIX + externalUserId;
        try {
            Object cachedObject = redisTemplate.opsForValue().get(key);
            if(cachedObject instanceof List) {
                List<McpConfig> cachedList = (List<McpConfig>) cachedObject;
                if (index > 0 && index <= cachedList.size()) {
                    return Optional.of(cachedList.get(index - 1));
                }
            }
        } catch(Exception e) {
            logger.error("从Redis缓存中读取MCP列表失败, Key: {}", key, e);
        }
        return Optional.empty();
    }


    
    public boolean hasPermission(String externalUserId, String mcpConnectionName) {
        return permissionRepository.existsByExternalUserIdAndMcpConfigName(externalUserId, mcpConnectionName);
    }
    
    public List<McpConfig> getAllMcpConfigs() {
        return mcpConfigRepository.findAll();
    }

    public Map<String, List<Long>> getPermissionsGroupedByUser() {
        return permissionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        UserMcpPermission::getExternalUserId,
                        Collectors.mapping(p -> p.getMcpConfig().getId(), Collectors.toList())
                ));
    }

    @Transactional
    public void grantPermission(String externalUserId, Long mcpConfigId) {
        if (permissionRepository.findByExternalUserIdAndMcpConfigId(externalUserId, mcpConfigId).isEmpty()) {
            McpConfig config = mcpConfigRepository.findById(mcpConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的 MCP 配置 ID"));
            UserMcpPermission permission = new UserMcpPermission();
            permission.setExternalUserId(externalUserId);
            permission.setMcpConfig(config);
            permissionRepository.save(permission);
            logger.info("已为用户 [{}] 授予对 MCP [{}] 的访问权限。", externalUserId, config.getName());
        }
    }

    @Transactional
    public void revokePermission(String externalUserId, Long mcpConfigId) {
        permissionRepository.deleteByExternalUserIdAndMcpConfigId(externalUserId, mcpConfigId);
        logger.info("已撤销用户 [{}] 对 MCP 配置 ID [{}] 的访问权限。", externalUserId, mcpConfigId);
    }
}