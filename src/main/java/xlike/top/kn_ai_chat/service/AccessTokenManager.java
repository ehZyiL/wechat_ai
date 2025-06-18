package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 微信 Access Token 管理器
 * <p>
 * 这个服务专门负责获取和缓存企业微信的 access_token。
 * 通过将其从 WeChatService 中分离，我们解决了循环依赖的问题。
 * 其他任何需要 access_token 的服务都应该注入这个管理器来获取。
 */
@Service
public class AccessTokenManager {

    private static final Logger logger = LoggerFactory.getLogger(AccessTokenManager.class);
    private static final String ACCESS_TOKEN_KEY_PREFIX = "wechat:access_token:";

    @Value("${wechat.corp-id}")
    private String corpId;

    @Value("${wechat.secret}")
    private String secret;

    private final RedisTemplate<String, String> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccessTokenManager(RedisTemplate<String, String> redisTemplate, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    /**
     * 获取企业微信的 Access Token。
     * <p>
     * 优先从 Redis 缓存中获取，如果缓存中没有或即将过期，则调用微信API重新获取并存入缓存。
     *
     * @return 有效的 Access Token 字符串。
     * @throws RuntimeException 如果从微信API获取Token失败或解析响应失败。
     */
    public String getAccessToken() {
        String key = ACCESS_TOKEN_KEY_PREFIX + this.corpId;
        String accessToken = redisTemplate.opsForValue().get(key);

        if (accessToken != null && !accessToken.isEmpty()) {
            return accessToken;
        }

        // 缓存中没有，调用API获取
        logger.info("缓存中无 access_token，正在从微信API获取...");
        String url = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=" + this.corpId + "&corpsecret=" + this.secret;
        String response = restTemplate.getForObject(url, String.class);
        logger.info("获取 Access Token 响应: {}", response);

        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.get("errcode").asInt() == 0) {
                String newAccessToken = root.get("access_token").asText();
                long expiresIn = root.get("expires_in").asLong();

                // 存入Redis，并设置比官方有效期短2分钟（120秒）的过期时间，以防止临界点问题
                redisTemplate.opsForValue().set(key, newAccessToken, expiresIn - 120, TimeUnit.SECONDS);
                return newAccessToken;
            } else {
                String errorMsg = root.get("errmsg").asText();
                throw new RuntimeException("获取 Access Token 失败: " + errorMsg);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析 Access Token 响应失败", e);
        }
    }
}