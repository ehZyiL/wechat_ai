package xlike.top.kn_ai_chat.tools.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.AiConfig;
import xlike.top.kn_ai_chat.operations.CreatePageOperation;
import xlike.top.kn_ai_chat.service.AiService;
import xlike.top.kn_ai_chat.service.NotionService;
import xlike.top.kn_ai_chat.service.UserConfigService;
import xlike.top.kn_ai_chat.tools.dto.NotionResponse;
import xlike.top.kn_ai_chat.tools.dto.OperationRequest;
import xlike.top.kn_ai_chat.tools.dto.PageTitleDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Agent 的核心工具集，用于与知识库（Notion）进行交互，并结合 RAG（检索增强生成）能力来处理和理解内容。
 * 这个工具不仅能执行创建、查询、追加内容等基本操作，还能基于特定页面乃至整个知识库的内容回答复杂问题。
 * @author Administrator
 */
@Component
public class NotionTool {

    private final NotionService notionService;
    private final AiService aiService;
    private final UserConfigService userConfigService;
    /**
     * 构造函数，通过依赖注入传入所需的服务。
     *
     * @param notionService     提供与 Notion 交互的服务。
     * @param aiService         提供 RAG 和 AI 处理能力的服务。
     * @param userConfigService 用于获取用户特定AI配置的服务。
     */
    public NotionTool(NotionService notionService, AiService aiService, UserConfigService userConfigService) {
        this.notionService = notionService;
        this.aiService = aiService;
        this.userConfigService = userConfigService;
    }

    /**
     * 从知识库中检索所有页面的完整信息列表。
     *
     * @return 返回一个包含所有页面详细信息的对象列表。
     */
    @Tool(name = "listAllPagesWithDetails", value = "从知识库中获取所有页面的详细信息列表。")
    public List<NotionResponse.Page> listAllPagesWithDetails() {
        return notionService.queryDatabasePages();
    }

    /**
     * 从知识库中获取所有页面的标题及其唯一ID。
     *
     * @return 返回一个包含页面ID和标题的对象列表。
     */
    @Tool(name = "listPageTitles", value = "获取知识库中所有页面的ID和标题列表。")
    public List<PageTitleDto> listPageTitles() {
        return notionService.queryDatabasePageTitles();
    }


    /**
     * 根据用户的问题，在整个知识库（Notion）中进行检索、整合并生成答案。
     * @param question 需要在知识库中寻找答案的问题。
     * @return 基于知识库内容生成的答案。
     */
    @Tool(name = "queryKnowledgeBase", value = "当需要基于内部知识库（如Notion文档、笔记）来回答问题时使用此工具。适用于总结、查询特定信息等场景。")
    public String queryKnowledgeBase(
            @P("需要在知识库中寻找答案的问题") String question
    ) {
        // 从Notion获取所有页面的内容，拼接成一个完整的知识库字符串
        List<PageTitleDto> allPages = notionService.queryDatabasePageTitles();
        if (allPages == null || allPages.isEmpty()) {
            return "知识库为空，没有任何内容可供查询。";
        }
        String fullKnowledgeBase = allPages.stream()
                .map(page -> {
                    List<String> content = notionService.getPageContent(page.getId());
                    if (content == null || content.isEmpty()) {
                        return "";
                    }
                    // 为每个页面内容添加标题
                    return "--- 来自文件: " + page.getTitle() + " ---\n" + String.join("\n", content);
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n"));
        if (fullKnowledgeBase.isBlank()) {
            return "知识库中的所有页面都为空，无法回答问题。";
        }
        AiConfig userAiConfig = userConfigService.getAiConfig("default");
        return aiService.executeRagAssistant(question, fullKnowledgeBase, userAiConfig);
    }

    /**
     * 基于知识库中特定页面的内容，回答相关问题。
     *
     * @param pageId         必需，知识库中页面的唯一ID。
     * @param question       必需，你想要基于此页面内容提出的问题。
     * @return 返回由RAG模型生成的答案。
     */
    @Tool(name = "answerQuestionFromPage", value = "基于知识库中特定页面的内容，回答相关问题。")
    public String answerQuestionFromPage(
            @P("知识库中页面的ID") String pageId,
            @P("需要回答的问题") String question) {
        AiConfig userAiConfig = userConfigService.getAiConfig("default");
        List<String> content = notionService.getPageContent(pageId);
        if (content == null || content.isEmpty()) {
            return "无法获取页面内容，或者页面内容为空。";
        }
        String knowledgeBase = String.join("\n", content);
        return aiService.executeRagAssistant(question, knowledgeBase, userAiConfig);
    }


    /**
     * 在知识库中创建一个带有标题和初始内容的新页面。
     *
     * @param title   必需，新页面的标题。
     * @param content 可选，页面内容，字符串列表，每个字符串为一段独立的 Markdown 格式文本。
     *                例如：
     *                [
     *                  "# 一级标题",
     *                  "这是一段普通段落。",
     *                  "## 二级标题",
     *                  "* 无序列表项1",
     *                  "1. 有序列表项1",
     *                  "> 引用内容",
     *                  "!!! 标注(Callout)",
     *                  ">> 可折叠列表",
     *                  "``````"
     *                ]
     *                这种结构方便对页面内容进行分段和格式化。
     * @return 创建成功后，返回新页面的详细信息。
     */
    @Tool(
            name = "createNewPage",
            value = "在知识库中创建一个包含标题和内容的新页面，内容需为分段的 Markdown 格式文本。内容传参示例：\n" +
                    "[\n" +
                    "  \"# 一级标题\",\n" +
                    "  \"这是一段普通段落。\",\n" +
                    "  \"## 二级标题\",\n" +
                    "  \"* 无序列表项1\",\n" +
                    "  \"1. 有序列表项1\",\n" +
                    "  \"> 引用内容\",\n" +
                    "  \"!!! 标注(Callout)\",\n" +
                    "  \">> 可折叠列表\",\n" +
                    "  \"``````\"\n" +
                    "]"
    )
    public NotionResponse.Page createNewPage(
            @P("新页面的标题") String title,
            @P("页面的内容，每段为独立的 Markdown 格式字符串。例如：\n" +
                    "[\"# 一级标题\", \"这是一段普通段落。\", \"## 二级标题\", \"* 无序列表项1\", \"1. 有序列表项1\", \"> 引用内容\", \"!!! 标注(Callout)\", \">> 可折叠列表\", \"``````\"]"
            ) List<String> content
    ) {
        OperationRequest.CreatePageRequestDto request = new OperationRequest.CreatePageRequestDto();
        request.setTitle(title);
        request.setContent(content);
        CreatePageOperation operation = new CreatePageOperation(request);
        return notionService.execute(operation);
    }
    /**
     * 向知识库中一个已存在的页面追加新的内容。
     *
     * @param pageId  必需，要追加内容的页面的ID。
     * @param content 必需，一个字符串列表，每个字符串都将作为一个新的段落追加到页面末尾。
     * @return 追加成功后，返回该页面当前所有的内容列表。
     */
    @Tool(name = "appendContentToPage", value = "向知识库中指定ID的页面追加新的内容段落。")
    public List<String> appendContentToPage(
            @P("要追加内容的页面的ID") String pageId,
            @P("要追加的段落内容列表") List<String> content) {
        return notionService.appendContentToPage(pageId, content);
    }
}