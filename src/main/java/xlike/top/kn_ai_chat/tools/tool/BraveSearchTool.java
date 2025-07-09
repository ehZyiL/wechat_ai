package xlike.top.kn_ai_chat.tools.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.tools.dto.BraveApiDTO;
import xlike.top.kn_ai_chat.utils.BraveSearchUtil;

/**
 * @author Administrator
 */
@Component
public class BraveSearchTool {

    private static final Logger logger = LoggerFactory.getLogger(BraveSearchTool.class);
    private final BraveSearchUtil braveSearchUtil;

    public BraveSearchTool(BraveSearchUtil braveSearchUtil) {
        this.braveSearchUtil = braveSearchUtil;
    }

    @Tool(
        name = "webSearch",
        value = "使用 Brave 搜索引擎进行通用的、实时的在线信息检索。适用于回答任何需要最新信息的问题、查找资料、定义术语或探索新主题。它会返回一个包含网页链接、常见问题(FAQ)和相关论坛讨论的综合摘要。"
    )
    public String search(@P("任何需要 搜索 的问题或关键词") String query) {
        logger.info("执行 Web 搜索, 查询: '{}'", query);
        try {
            BraveApiDTO.WebSearchApiResponse response = braveSearchUtil.performWebSearch(query);
            return formatSearchResponse(response);
        } catch (Exception e) {
            logger.error("Web 搜索工具在执行查询 '{}' 时发生错误", query, e);
            return "抱歉，在为您执行在线搜索时遇到了网络或服务问题，请稍后重试。";
        }
    }

    /**
     * 智能格式化搜索结果，将不同类型的信息组合成一个易于阅读的摘要。
     * @param response 从 Brave API 返回的完整响应对象
     * @return 格式化后的字符串
     */
    private String formatSearchResponse(BraveApiDTO.WebSearchApiResponse response) {
        if (response == null) {
            return "抱歉，未能获取到任何搜索结果。";
        }

        StringBuilder sb = new StringBuilder();
        String originalQuery = response.getQuery() != null ? response.getQuery().getOriginal() : "";
        sb.append("✅ 这是关于 “").append(originalQuery).append("” 的搜索结果摘要：\n\n");
        if (response.getFaq() != null && !response.getFaq().getResults().isEmpty()) {
            sb.append("🤔 **常见问题 (FAQ)**\n");
            response.getFaq().getResults().stream().limit(3).forEach(qa -> {
                sb.append("- **问**: ").append(qa.getQuestion()).append("\n");
                sb.append("  **答**: ").append(qa.getAnswer()).append("\n");
            });
            sb.append("\n");
        }

        //格式化网页搜索结果
        if (response.getWeb() != null && !response.getWeb().getResults().isEmpty()) {
            sb.append("📄 **相关网页**\n");
            response.getWeb().getResults().stream().limit(4).forEach(res -> {
                sb.append("- **").append(res.getTitle()).append("**\n");
                sb.append("  摘要: ").append(res.getDescription()).append("\n");
                sb.append("  链接: ").append(res.getUrl()).append("\n");
            });
            sb.append("\n");
        }

        // 格式化论坛讨论
        if (response.getDiscussions() != null && !response.getDiscussions().getResults().isEmpty()) {
            sb.append("💬 **相关讨论**\n");
            response.getDiscussions().getResults().stream().limit(3).forEach(cluster -> {
                cluster.getResults().forEach(disc -> {
                    sb.append("- ").append(disc.getTitle()).append("\n");
                    sb.append("  链接: ").append(disc.getUrl()).append("\n");
                });
            });
        }
        
        // 如果没有任何内容，返回提示
        if (sb.toString().trim().endsWith("摘要：")) {
             return "关于 “" + originalQuery + "”，我没有找到具体的摘要信息。";
        }

        return sb.toString();
    }
}