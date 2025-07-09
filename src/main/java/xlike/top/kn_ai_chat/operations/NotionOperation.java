package xlike.top.kn_ai_chat.operations;


import xlike.top.kn_ai_chat.service.NotionService;

/**
 * Notion操作的通用接口 (命令模式)。
 * @author Administrator
 * @param <T> 此操作预期的返回类型
 */
@FunctionalInterface
public interface NotionOperation<T> {
    /**
     * 执行具体操作。
     * @param notionService 提供执行操作所需依赖的服务
     * @return 操作结果
     */
    T execute(NotionService notionService);
}