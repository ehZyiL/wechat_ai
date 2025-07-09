package xlike.top.kn_ai_chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import xlike.top.kn_ai_chat.operations.NotionOperation;
import xlike.top.kn_ai_chat.tools.dto.NotionResponse;
import xlike.top.kn_ai_chat.tools.dto.PageTitleDto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Notion服务类，封装了所有与Notion API交互的底层逻辑。
 * 通过RestTemplate实现HTTP请求的发送。
 * @author Administrator
 */
@Service
public class NotionService {


    /**
     * Notion API的密钥，从配置文件中读取。
     */
    @Value("${tools.notion.api.key}")
    private String apiKey;

    /**
     * Notion API的版本号，从配置文件中读取。
     */
    @Value("${tools.notion.api.version}")
    private String apiVersion;

    /**
     * Notion API的基础URL，从配置文件中读取。
     */
    @Value("${tools.notion.api.base-url}")
    private String baseUrl;

    /**
     * Notion数据库的ID，从配置文件中读取。
     */
    @Value("${tools.notion.database.id}")
    private String databaseId;

    /**
     * Spring的RestTemplate实例，用于发送HTTP请求。
     */
    private final RestTemplate restTemplate;

    /**
     * 构造函数，注入RestTemplate。
     * @param restTemplate RestTemplate的实例
     */
    public NotionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    /**
     * 执行任何传入的Notion操作。
     * Controller会建立一个具体的操作对象，并调用此方法。
     * @param operation 具体的操作 (例如 CreatePageOperation)
     * @param <T> 操作的返回类型
     * @return 操作的执行结果
     */
    public <T> T execute(NotionOperation<T> operation) {
        return operation.execute(this);
    }


    /**
     * 获取数据库所有页面的 ID 和标题。
     * @return 包含页面ID和标题的 DTO 列表
     */
    public List<PageTitleDto> queryDatabasePageTitles() {
        String url = baseUrl + "/databases/" + databaseId + "/query";
        HttpEntity<String> requestEntity = new HttpEntity<>("{}", createHeaders());
        try {
            ResponseEntity<NotionResponse.QueryResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    NotionResponse.QueryResponse.class
            );
            if (response.getBody() == null || response.getBody().getResults() == null) {
                return Collections.emptyList();
            }
            // 直接在这里处理返回结果，将 Page 对象映射为 PageTitleDto 对象
            return response.getBody().getResults().stream()
                    .map(page -> {
                        String pageId = page.getId();
                        String pageTitle = page.getProperties().entrySet().stream()
                                .filter(entry -> "title".equals(entry.getValue().getType()))
                                .findFirst()
                                .map(entry -> {
                                    List<NotionResponse.RichText> titleList = entry.getValue().getTitle();
                                    if (titleList != null && !titleList.isEmpty()) {
                                        return titleList.getFirst().getPlainText();
                                    }
                                    return "（无标题）";
                                })
                                .orElse("（无标题）");

                        return new PageTitleDto(pageId, pageTitle);
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("查询数据库时出错: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 在数据库中建立一个新页面，并同时添加内容。
     * @param title 页面标题
     * @param titlePropertyName 标题属性的名称
     * @param content 一个字符串列表，每个字符串将成为一个段落
     * @return 创建成功则返回新页面的信息，否则返回null
     */
    public NotionResponse.Page createPage(String title, String titlePropertyName, List<String> content) {
        String url = baseUrl + "/pages";

        // 构建属性
        Map<String, Object> titleProperty = Map.of("title", List.of(Map.of("text", Map.of("content", title))));
        Map<String, Object> properties = Map.of(titlePropertyName, titleProperty);

        // 构建子内容
        List<Map<String, Object>> children = (content != null && !content.isEmpty()) ?
                content.stream().map(this::createBlockFromMarkdown).collect(Collectors.toList()) : null;

        // 构建最终请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("parent", Map.of("database_id", databaseId));
        requestBody.put("properties", properties);
        if (children != null) {
            requestBody.put("children", children);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, createHeaders());
        try {
            return restTemplate.postForEntity(url, requestEntity, NotionResponse.Page.class).getBody();
        } catch (Exception e) {
            System.err.println("创建页面时出错: " + e.getMessage());
            return null;
        }
    }


    /**
     * 向指定页面追加新的内容块，并返回追加后页面的全部内容。
     * @param pageId 要追加内容的页面ID
     * @param content 一个字符串列表，每个字符串将成为一个新的段落
     * @return 成功则返回包含页面所有内容的列表，失败则返回null
     */
    public List<String> appendContentToPage(String pageId, List<String> content) {
        String url = baseUrl + "/blocks/" + pageId + "/children";

        // 将 Markdown 字符串列表转换为 Notion Block Map 列表
        List<Map<String, Object>> childrenToAppend = content.stream()
                .map(this::createBlockFromMarkdown)
                .toList();

        Map<String, List<Map<String, Object>>> requestBody = Map.of("children", childrenToAppend);
        HttpEntity<Map<String, List<Map<String, Object>>>> requestEntity = new HttpEntity<>(requestBody, createHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, String.class);
            return getPageContent(pageId);
        } catch (Exception e) {
            System.err.println("追加页面内容时出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 决定性修正：手动从 Markdown 创建一个纯净的 Block Map 对象
     * @param markdownString 单行 Markdown 文本
     * @return 一个符合 Notion API 格式的 Map 对象
     */
    private Map<String, Object> createBlockFromMarkdown(String markdownString) {
        Map<String, Object> block = new HashMap<>();
        block.put("object", "block");

        Pattern numberedListPattern = Pattern.compile("^\\d+\\. (.*)");
        Matcher numberedListMatcher = numberedListPattern.matcher(markdownString);

        if (markdownString.startsWith("```")) {
            block.put("type", "code");
            String content = markdownString.substring(3);
            if (content.endsWith("```")) { content = content.substring(0, content.length() - 3).trim(); }
            String language = "plain text";
            int firstLineBreak = content.indexOf('\n');
            if (firstLineBreak != -1 && firstLineBreak < 20) {
                language = content.substring(0, firstLineBreak).trim();
                content = content.substring(firstLineBreak + 1);
            }
            Map<String, Object> codeObject = Map.of(
                    "rich_text", List.of(Map.of("type", "text", "text", Map.of("content", content))),
                    "language", language
            );
            block.put("code", codeObject);
        } else if (markdownString.startsWith("# ")) {
            block.put("type", "heading_1");
            block.put("heading_1", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(2))))));
        } else if (markdownString.startsWith("## ")) {
            block.put("type", "heading_2");
            block.put("heading_2", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(3))))));
        } else if (markdownString.startsWith("### ")) {
            block.put("type", "heading_3");
            block.put("heading_3", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(4))))));
        } else if ("---".equals(markdownString)) {
            block.put("type", "divider");
            block.put("divider", new HashMap<>());
        } else if (markdownString.startsWith("[ ] ")) {
            block.put("type", "to_do");
            block.put("to_do", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(4)))), "checked", false));
        } else if (markdownString.startsWith("[x] ")) {
            block.put("type", "to_do");
            block.put("to_do", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(4)))), "checked", true));
        } else if (markdownString.startsWith("* ") || markdownString.startsWith("- ")) {
            block.put("type", "bulleted_list_item");
            block.put("bulleted_list_item", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(2))))));
        } else if (numberedListMatcher.matches()) {
            block.put("type", "numbered_list_item");
            block.put("numbered_list_item", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", numberedListMatcher.group(1))))));
        } else if (markdownString.startsWith("> ")) {
            block.put("type", "quote");
            block.put("quote", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(2))))));
        }
        else if (markdownString.startsWith(">> ")) {
            block.put("type", "toggle");
            block.put("toggle", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(3))))));
        } else if (markdownString.startsWith("!!! ")) {
            block.put("type", "callout");
            block.put("callout", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString.substring(4))))));
        }
        else { // 默认情况
            block.put("type", "paragraph");
            block.put("paragraph", Map.of("rich_text", List.of(Map.of("type", "text", "text", Map.of("content", markdownString)))));
        }
        return block;
    }



    /**
     * 获取一个页面内部所有的段落文字。
     * @param pageId 要查询的页面ID
     * @return 包含页面内容的字符串列表
     */
    public List<String> getPageContent(String pageId) {
        String url = baseUrl + "/blocks/" + pageId + "/children";
        HttpEntity<String> requestEntity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<NotionResponse.BlockChildrenResponse> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, NotionResponse.BlockChildrenResponse.class);
            if (response.getBody() == null || response.getBody().getResults() == null) {
                return Collections.emptyList();
            }
            List<String> contentList = new ArrayList<>();
            // 使用传统的 for-each 循环，因为它更适合处理需要额外 API 调用的情况
            for (NotionResponse.Block block : response.getBody().getResults()) {
                String blockContent = processBlock(block);
                if (blockContent != null && !blockContent.isEmpty()) {
                    contentList.add(blockContent);
                }
            }
            return contentList;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }




    /**
     * 辅助方法：处理单个 Block 对象，将其转换为字符串。
     * @param block 要处理的块
     * @return 转换后的字符串，如果是不支持的类型则返回 null
     */
    private String processBlock(NotionResponse.Block block) {
        // 定义一个可复用的函数，用于从富文本(RichText)列表中提取纯文本
        java.util.function.Function<List<NotionResponse.RichText>, String> extractText =
                rt -> rt.stream().map(NotionResponse.RichText::getPlainText).collect(Collectors.joining());

        switch (block.getType()) {
            case "paragraph":
                return block.getParagraph() != null ? extractText.apply(block.getParagraph().getRichText()) : null;

            case "heading_1":
                return block.getHeading1() != null ? "# " + extractText.apply(block.getHeading1().getRichText()) : null;

            case "heading_2":
                return block.getHeading2() != null ? "## " + extractText.apply(block.getHeading2().getRichText()) : null;

            case "heading_3":
                return block.getHeading3() != null ? "### " + extractText.apply(block.getHeading3().getRichText()) : null;

            case "bulleted_list_item":
                return block.getBulletedListItem() != null ? "* " + extractText.apply(block.getBulletedListItem().getRichText()) : null;

            case "numbered_list_item":
                return block.getNumberedListItem() != null ? "1. " + extractText.apply(block.getNumberedListItem().getRichText()) : null;

            case "to_do":
                if (block.getToDo() == null) {
                    return null;
                }
                String prefix = block.getToDo().isChecked() ? "[x] " : "[ ] ";
                return prefix + extractText.apply(block.getToDo().getRichText());

            case "quote":
                return block.getQuote() != null ? "> " + extractText.apply(block.getQuote().getRichText()) : null;

            case "code":
                if (block.getCode() == null) {
                    return null;
                }
                String language = block.getCode().getLanguage() != null ? block.getCode().getLanguage() : "";
                String codeContent = extractText.apply(block.getCode().getRichText());
                return "```" + language + "\n" + codeContent + "\n```";

            case "callout":
                return block.getCallout() != null ? "💡 " + extractText.apply(block.getCallout().getRichText()) : null;

            case "toggle":
                return block.getToggle() != null ? "▶️ " + extractText.apply(block.getToggle().getRichText()) : null;

            case "table":
                return fetchAndFormatTable(block.getId(), extractText);
            default:
                return null;
        }
    }

    /**
     * 辅助方法：获取表格内容并格式化为 Markdown
     * @param tableBlockId 表格块的ID
     * @param extractText 用于提取文本的函数
     * @return 格式化后的 Markdown 表格字符串
     */
    private String fetchAndFormatTable(String tableBlockId, java.util.function.Function<List<NotionResponse.RichText>, String> extractText) {
        String tableUrl = baseUrl + "/blocks/" + tableBlockId + "/children";
        HttpEntity<String> requestEntity = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<NotionResponse.BlockChildrenResponse> response = restTemplate.exchange(tableUrl, HttpMethod.GET, requestEntity, NotionResponse.BlockChildrenResponse.class);
            if (response.getBody() == null || response.getBody().getResults() == null) {
                return "[获取表格内容失败]";
            }
            StringBuilder tableMarkdown = new StringBuilder();
            boolean firstRow = true;
            for (NotionResponse.Block rowBlock : response.getBody().getResults()) {
                if ("table_row".equals(rowBlock.getType()) && rowBlock.getTableRow() != null) {
                    // 这里的处理逻辑需要调整，以匹配新的 List<List<RichText>> 结构
                    String rowContent = rowBlock.getTableRow().getCells().stream()
                            .map(extractText)
                            .collect(Collectors.joining(" | "));
                    tableMarkdown.append("| ").append(rowContent).append(" |\n");

                    // 如果是第一行，自动在下面添加 Markdown 表格的分割线
                    if (firstRow && !rowContent.isEmpty()) {
                        String separator = rowBlock.getTableRow().getCells().stream()
                                .map(cell -> "---")
                                .collect(Collectors.joining(" | "));
                        tableMarkdown.append("| ").append(separator).append(" |\n");
                        firstRow = false;
                    }
                }
            }
            return tableMarkdown.toString();

        } catch (Exception e) {
            return "[]";
        }
    }


    /**
     * 创建通用的请求头。
     * @return 包含认证和版本信息的HttpHeaders对象
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("Notion-Version", apiVersion);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }


    /**
     * 获取数据库所有页面（包含ID和属性）。
     * @return 页面对象的列表
     */
    public List<NotionResponse.Page> queryDatabasePages() {
        String url = baseUrl + "/databases/" + databaseId + "/query";
        HttpEntity<String> requestEntity = new HttpEntity<>("{}", createHeaders());

        try {
            ResponseEntity<NotionResponse.QueryResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    NotionResponse.QueryResponse.class
            );
            // 如果响应体不为空，则返回结果列表，否则返回空列表
            return response.getBody() != null ? response.getBody().getResults() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}