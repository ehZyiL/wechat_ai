package xlike.top.kn_ai_chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.domain.WeChatKfAccount;
import xlike.top.kn_ai_chat.repository.WeChatKfAccountRepository;

import java.util.Optional;

/**
 * @author xlike
 */
@Service
public class WeChatKfAccountService {

    private static final Logger logger = LoggerFactory.getLogger(WeChatKfAccountService.class);

    private final AccessTokenManager accessTokenManager;
    private final RestTemplate restTemplate;
    private final WeChatKfAccountRepository kfAccountRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeChatKfAccountService(AccessTokenManager accessTokenManager,
                                  RestTemplate restTemplate,
                                  WeChatKfAccountRepository kfAccountRepository) {
        this.accessTokenManager = accessTokenManager;
        this.restTemplate = restTemplate;
        this.kfAccountRepository = kfAccountRepository;
    }

    /**
     * 项目启动时执行此方法，用于获取并存储客服账号信息。
     * 标记为 @PostConstruct 注解的方法将在依赖注入完成后自动执行。
     */
    @PostConstruct
    @Transactional
    public void initKfAccount() {
        logger.info("开始获取微信客服账号列表...");

        try {
            // 获取AccessToken
            String accessToken = accessTokenManager.getAccessToken();
            String url = "https://qyapi.weixin.qq.com/cgi-bin/kf/account/list?access_token=" + accessToken;
            String response = restTemplate.postForObject(url, "{}", String.class);
            logger.info("获取微信客服账号列表API响应: {}", response);
            JsonNode root = objectMapper.readTree(response);
            if (root.path("errcode").asInt() == 0 && root.has("account_list")) {
                JsonNode accountList = root.path("account_list");
                if (accountList.isArray() && !accountList.isEmpty()) {
                    //获取第一个客服账号
                    JsonNode firstAccountNode = accountList.get(0);
                    WeChatKfAccount kfAccount = new WeChatKfAccount();
                    kfAccount.setOpenKfid(firstAccountNode.path("open_kfid").asText());
                    kfAccount.setName(firstAccountNode.path("name").asText());
                    kfAccount.setAvatar(firstAccountNode.path("avatar").asText());
                    //清空旧数据并保存新数据
                    kfAccountRepository.deleteAllInBatch();
                    kfAccountRepository.save(kfAccount);

                    logger.info("成功获取并保存了第一个客服账号: {}", kfAccount.getName());
                } else {
                    logger.warn("API返回的客服账号列表为空。");
                }
            } else {
                logger.error("获取客服账号列表失败: {}", root.path("errmsg").asText());
            }
        } catch (Exception e) {
            logger.error("获取并存储微信客服账号时发生异常", e);
        }
    }

    /**
     * 无参方法，用于获取存储在数据库中的客服账号的 open_kfid。
     *
     * @return 返回客服的 open_kfid，如果数据库中没有数据则返回 null。
     */
    public String getOpenKfid() {
        Optional<WeChatKfAccount> accountOpt = kfAccountRepository.findAll().stream().findFirst();
        if (accountOpt.isPresent()) {
            return accountOpt.get().getOpenKfid();
        } else {
            logger.warn("数据库中没有存储任何客服账号信息，无法获取open_kfid。");
            return null;
        }
    }
}