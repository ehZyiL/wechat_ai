package xlike.top.kn_ai_chat.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 一个简单的数据传输对象，用于封装页面的ID和标题。
 * @author Administrator
 */
@Data
@AllArgsConstructor
public class PageTitleDto {
    private String id;
    private String title;
}