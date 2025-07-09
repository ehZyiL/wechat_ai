package xlike.top.kn_ai_chat.tools.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 这个文件使用内部类来映射 Notion API 返回的复杂 JSON 结构。
 * @author Administrator
 */
public class NotionResponse {

    // 通用响应结构
    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QueryResponse { private List<Page> results; }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlockChildrenResponse { private List<Block> results; }

    // 页面与属性
    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Page {
        private String id;
        private Map<String, Property> properties;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {
        private String id;
        private String type;
        private List<RichText> title;
    }

    // Block 块与各种类型
    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Block {
        private String id;
        private String type;
        private boolean hasChildren;
        private Paragraph paragraph;
        private Code code;
        @JsonProperty("heading_1") private Heading heading1;
        @JsonProperty("heading_2") private Heading heading2;
        @JsonProperty("heading_3") private Heading heading3;
        @JsonProperty("bulleted_list_item") private ListItem bulletedListItem;
        @JsonProperty("numbered_list_item") private ListItem numberedListItem;
        @JsonProperty("to_do") private ToDo toDo;
        private Quote quote;
        private Callout callout;
        private Toggle toggle;
        private Table table;
        @JsonProperty("table_row") private TableRow tableRow;
    }

    // 内容块的具体实现
    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RichText {
        @JsonProperty("plain_text") private String plainText;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paragraph {
        @JsonProperty("rich_text") private List<RichText> richText;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Code {
        @JsonProperty("rich_text") private List<RichText> richText;
        private String language;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Heading {
        @JsonProperty("rich_text") private List<RichText> richText;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown =true)
    public static class ListItem {
        @JsonProperty("rich_text") private List<RichText> richText;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToDo {
        @JsonProperty("rich_text") private List<RichText> richText;
        private boolean checked;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Quote {
        @JsonProperty("rich_text") private List<RichText> richText;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Callout {
        @JsonProperty("rich_text") private List<RichText> richText;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Toggle {
        @JsonProperty("rich_text") private List<RichText> richText;
    }

    // 表格相关类
    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Table {
        @JsonProperty("table_width") private int tableWidth;
        @JsonProperty("has_column_header") private boolean hasColumnHeader;
        @JsonProperty("has_row_header") private boolean hasRowHeader;
    }

    @Data @NoArgsConstructor @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TableRow {
        private List<List<RichText>> cells;
    }

}