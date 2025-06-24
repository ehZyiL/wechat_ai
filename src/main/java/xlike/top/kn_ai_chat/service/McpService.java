package xlike.top.kn_ai_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.McpConfig;
import xlike.top.kn_ai_chat.domain.UserMcpPermission;
import xlike.top.kn_ai_chat.domain.WeChatUser;
import xlike.top.kn_ai_chat.repository.McpConfigRepository;
import xlike.top.kn_ai_chat.repository.UserMcpPermissionRepository;
import xlike.top.kn_ai_chat.repository.WeChatUserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Service
public class McpService {

    private static final Logger logger = LoggerFactory.getLogger(McpService.class);
    private static final String MCP_CONNECTIONS_PREFIX = "spring.ai.mcp.client.sse.connections.";
    private static final String MCP_CACHE_KEY_PREFIX = "kn_ai_chat:mcp:list:";
    private static final long CACHE_TIMEOUT_MINUTES = 5;

    private final McpConfigRepository mcpConfigRepository;
    private final UserMcpPermissionRepository permissionRepository;
    private final ConfigurableEnvironment environment;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatClient chatClient;
    private final WeChatUserRepository weChatUserRepository;

    public McpService(McpConfigRepository mcpConfigRepository,
                      UserMcpPermissionRepository permissionRepository,
                      ConfigurableEnvironment environment,
                      RedisTemplate<String, Object> redisTemplate,
                      ChatClient.Builder chatClientBuilder,
                      ToolCallbackProvider toolCallbackProvider,
                      WeChatUserRepository weChatUserRepository) {
        this.mcpConfigRepository = mcpConfigRepository;
        this.permissionRepository = permissionRepository;
        this.environment = environment;
        this.redisTemplate = redisTemplate;
        this.weChatUserRepository = weChatUserRepository;
        // 这里就是调用MCP服务必需品
        this.chatClient = chatClientBuilder
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }

    @Transactional
    public String executeMcpRequest(String externalUserId, String mcpConnectionName, String prompt) {
        if (!hasPermission(externalUserId, mcpConnectionName)) {
            logger.warn("用户 [{}] 尝试访问未授权的 MCP 连接 [{}]", externalUserId, mcpConnectionName);
            return "❌ 您没有权限使用名为 '" + mcpConnectionName + "' 的模型服务。请联系管理员授权。";
        }
        
        try {
            logger.info("用户 [{}] 通过 MCP 连接 [{}] 发起请求，参数是：{}", externalUserId, mcpConnectionName, prompt);
            return this.chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

        } catch (Exception e) {
            logger.error("调用 MCP 连接 [{}] 时发生未知错误，用户: {}。错误: {}", mcpConnectionName, externalUserId, e.getMessage(), e);
            return "调用模型服务 '" + mcpConnectionName + "' 时发生内部错误: " + e.getClass().getSimpleName();
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
    
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadMcpConnectionsFromProperties() {
        logger.info("正在从配置文件加载 MCP 连接信息到数据库...");
        environment.getPropertySources().stream()
                .filter(ps -> ps instanceof MapPropertySource)
                .map(ps -> ((MapPropertySource) ps).getSource().keySet())
                .flatMap(set -> set.stream()
                        .filter(key -> key.startsWith(MCP_CONNECTIONS_PREFIX) && key.endsWith(".url")))
                .forEach(key -> {
                    String name = key.substring(MCP_CONNECTIONS_PREFIX.length(), key.lastIndexOf(".url"));
                    String urlKey = MCP_CONNECTIONS_PREFIX + name + ".url";
                    String endpointKey = MCP_CONNECTIONS_PREFIX + name + ".sse-endpoint";

                    String url = environment.getProperty(urlKey);
                    String endpoint = environment.getProperty(endpointKey);

                    if (url != null && endpoint != null) {
                        mcpConfigRepository.findByName(name).or(() -> {
                            logger.info("发现新的 MCP 配置: [{}], URL: {}, Endpoint: {}", name, url, endpoint);
                            McpConfig newConfig = new McpConfig();
                            newConfig.setName(name);
                            newConfig.setUrl(url);
                            newConfig.setSseEndpoint(endpoint);
                            mcpConfigRepository.save(newConfig);
                            return Optional.of(newConfig);
                        });
                    }
                });
        logger.info("MCP 连接信息加载完成。");
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
        if (!permissionRepository.findByExternalUserIdAndMcpConfigId(externalUserId, mcpConfigId).isPresent()) {
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
