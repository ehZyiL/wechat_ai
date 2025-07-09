package xlike.top.kn_ai_chat.tools.tool;

import com.google.api.services.customsearch.v1.model.Result;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.utils.GoogleSearchUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Administrator
 */
@Component
public class GoogleSearchTool {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSearchTool.class);
    private final GoogleSearchUtil googleSearchUtil;

    public GoogleSearchTool(GoogleSearchUtil googleSearchUtil) {
        this.googleSearchUtil = googleSearchUtil;
    }

    @Tool(
        name = "googleWebSearch",
        value = "优先使用！！！，使用 Google 搜索引擎进行权威、精准的在线信息检索。适用于需要查找可靠资料、回答事实性问题、获取官方链接或进行深入研究的场景。它会返回一个包含标题、链接和摘要的搜索结果列表。"
    )
    public String search(@P("任何需要在线搜索的问题或关键词") String query) {
        logger.info("执行 Google Web 搜索, 查询: '{}'", query);
        try {
            List<Result> results = googleSearchUtil.performSearch(query);
            return formatResponse(query, results);
        } catch (Exception e) {
            logger.error("Google 搜索工具在执行查询 '{}' 时发生错误", query, e);
            // 将更具体的错误信息返回给 AI，以便调试
            return "抱歉，调用 Google 搜索 API 时遇到问题: " + e.getMessage();
        }
    }

    private String formatResponse(String query, List<Result> results) {
        if (results == null || results.isEmpty()) {
            return "关于 “" + query + "”，我没有找到相关的搜索结果。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("✅ 这是通过 Google 为您找到的关于 “").append(query).append("” 的搜索结果：\n\n");

        // 只显示前 5 条结果，以保持简洁
        results.stream().limit(5).forEach(result -> {
            sb.append("📄 **标题**: ").append(result.getTitle()).append("\n");
            sb.append("🔗 **链接**: ").append(result.getLink()).append("\n");
            sb.append("📝 **摘要**: ").append(result.getSnippet()).append("\n\n");
        });

        return sb.toString();
    }
}