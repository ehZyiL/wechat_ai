package xlike.top.kn_ai_chat.tools.dto;

import lombok.Data;

import java.util.List;

/**
 * 用于封装操作请求的数据传输对象 (DTO)。
 * @author Administrator
 */
public class OperationRequest {

    /**
     * 专用于新增页面的请求DTO。
     */
    @Data
    public static class CreatePageRequestDto {
        private String title;
        // 默认的标题列名，如果您的数据库标题列是这个名字，请求时就不用传这个字段了
        private String titlePropertyName = "名称";
        // 新增一个 content 字段，用来接收页面内容
        private List<String> content;
    }

    @Data
    public static class AppendContentRequestDto {
        // 定义一个 content 字段，用来接收要追加的段落列表
        private List<String> content;
    }
}