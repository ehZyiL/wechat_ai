package xlike.top.kn_ai_chat.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import xlike.top.kn_ai_chat.tools.dto.BraveApiDTO;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author Administrator
 */
@Component
public class BraveSearchUtil {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String braveApiKey;

    private static final String BRAVE_SEARCH_API_URL = "https://api.search.brave.com/res/v1/web/search";

    /**
     * 构造函数 - 在这里完成代理的配置和 RestTemplate 的实例化。
     * 它不再接收 RestTemplate Bean，而是自己创建。
     *
     * @param objectMapper Spring 自动注入 ObjectMapper
     * @param braveApiKey  Spring 从 application.yml 注入 API Key
     */
    public BraveSearchUtil(ObjectMapper objectMapper, @Value("${tools.brave.api-key}") String braveApiKey) {
        this.objectMapper = objectMapper;
        this.braveApiKey = braveApiKey;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
//        requestFactory.setProxy(proxy);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    /**
     * 执行 Brave Web Search API 请求。
     * 此方法现在会自动使用上面构造函数中配置好的、带代理的 RestTemplate。
     *
     * @param query 搜索查询词
     * @return 解析后的 WebSearchApiResponse DTO
     * @throws Exception 如果 API 调用或解析失败
     */
    public BraveApiDTO.WebSearchApiResponse performWebSearch(String query) throws Exception {
        if (braveApiKey == null || braveApiKey.isBlank()) {
            throw new IllegalStateException("Brave Search API Key 未在配置中提供。");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Subscription-Token", braveApiKey);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.set("Accept-Encoding", "gzip");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = UriComponentsBuilder.fromHttpUrl(BRAVE_SEARCH_API_URL)
                .queryParam("q", query)
                .toUriString();

        try {
            // 这里的 restTemplate 实例就是我们带代理的那个实例
            ResponseEntity<String> response = this.restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return objectMapper.readValue(response.getBody(), BraveApiDTO.WebSearchApiResponse.class);
        } catch (Exception e) {
            // 将异常向上层抛出，由 Tool 层进行用户友好的处理
            throw new RuntimeException("调用 Brave Web Search API 失败: " + e.getMessage(), e);
        }
    }
}