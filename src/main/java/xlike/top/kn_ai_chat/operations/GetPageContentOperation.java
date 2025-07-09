package xlike.top.kn_ai_chat.operations;

import lombok.RequiredArgsConstructor;
import xlike.top.kn_ai_chat.service.NotionService;

import java.util.List;

/**
 * 获取页面内容 ConcreteOperation，实现 NotionOperation 接口。
 * @author Administrator
 */
@RequiredArgsConstructor
public class GetPageContentOperation implements NotionOperation<List<String>> {

    /**
     * 要获取内容的页面ID。
     */
    private final String pageId;

    /**
     * 执行获取页面内容的操作。
     * @param notionService Notion服务的实例
     * @return 包含页面内容的字符串列表
     */
    @Override
    public List<String> execute(NotionService notionService) {
        return notionService.getPageContent(pageId);
    }
}