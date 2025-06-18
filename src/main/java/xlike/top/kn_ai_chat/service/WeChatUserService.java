package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.domain.WeChatUser;
import xlike.top.kn_ai_chat.repository.WeChatUserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 负责处理微信用户相关业务的服务.
 * 包括用户的创建、信息同步以及状态管理.
 * @author xlike
 */
@Service
public class WeChatUserService {

    private static final Logger logger = LoggerFactory.getLogger(WeChatUserService.class);
    private final WeChatUserRepository userRepository;
    private final AccessTokenManager accessTokenManager;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeChatUserService(WeChatUserRepository userRepository, AccessTokenManager accessTokenManager, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.accessTokenManager = accessTokenManager;
        this.restTemplate = restTemplate;
    }

    /**
     * 获取或创建用户。如果用户在本地数据库不存在，则调用API获取信息并创建新用户.
     * @param externalUserId 用户的 external_userid
     */
    public void getOrCreateUser(String externalUserId) {
        if (!userRepository.existsById(externalUserId)) {
            logger.info("用户 [{}] 不存在于数据库中，尝试从企业微信API获取信息...", externalUserId);
            fetchAndSaveUserFromApi(externalUserId);
        }
    }

    /**
     * 调用企业微信API获取用户基本信息并保存到数据库.
     * 该方法会根据提供的PDF文档调用获取客户基础信息接口.
     * @param externalUserId 用户的 external_userid
     */
    private void fetchAndSaveUserFromApi(String externalUserId) {
        String accessToken = accessTokenManager.getAccessToken();
        String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/customer/batchget?access_token=" + accessToken;

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("external_userid_list", Collections.singletonList(externalUserId));
        requestBody.put("need_enter_session_context", 0);

        try {
            String responseStr = restTemplate.postForObject(url, requestBody, String.class);
            JsonNode root = objectMapper.readTree(responseStr);

            if (root.path("errcode").asInt() == 0 && root.has("customer_list")) {
                JsonNode customerList = root.path("customer_list");
                if (customerList.isArray() && !customerList.isEmpty()) {
                    JsonNode customerNode = customerList.get(0);
                    WeChatUser user = new WeChatUser();
                    user.setExternalUserId(customerNode.path("external_userid").asText());
                    user.setNickname(customerNode.path("nickname").asText());
                    user.setAvatar(customerNode.path("avatar").asText());
                    user.setInfo(customerNode.toString());
                    user.setBlocked(false);
                    user.setLastUpdated(LocalDateTime.now());
                    
                    userRepository.save(user);
                    logger.info("成功获取并保存了用户 [{}] 的信息。", externalUserId);
                }
            } else {
                logger.error("从企微API获取用户 [{}] 信息失败: {}", externalUserId, responseStr);
            }
        } catch (Exception e) {
            logger.error("调用企微API获取用户 [{}] 信息时发生异常", externalUserId, e);
        }
    }
    
    /**
     * 检查用户是否被拉黑.
     * @param externalUserId 用户ID
     * @return 如果用户存在且被拉黑则返回 true, 否则返回 false.
     */
    public boolean isUserBlocked(String externalUserId) {
        Optional<WeChatUser> userOpt = userRepository.findById(externalUserId);
        return userOpt.map(WeChatUser::isBlocked).orElse(false);
    }
}