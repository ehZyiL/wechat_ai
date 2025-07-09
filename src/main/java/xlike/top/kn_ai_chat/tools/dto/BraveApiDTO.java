package xlike.top.kn_ai_chat.tools.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 专门用于映射 Brave Web Search API 的响应 (v1/web/search)。
 * @author Administrator
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BraveApiDTO {

    /**
     * Web Search API 的顶层响应模型。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebSearchApiResponse {
        private Query query;
        private Faq faq;
        private Discussions discussions;
        private Web web;
    }

    // 各个响应部分的具体

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        private String original;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Faq {
        private List<QuestionAnswer> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionAnswer {
        private String question;
        private String answer;
        private String title;
        private String url;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Discussions {
        private List<Cluster> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cluster {
        private String title;
        private List<DiscussionResult> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DiscussionResult {
        private String title;
        private String url;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Web {
        private List<WebResult> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebResult {
        private String title;
        private String url;
        private String description;
    }
}