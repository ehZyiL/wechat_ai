package xlike.top.kn_ai_chat.operations;

import lombok.RequiredArgsConstructor;
import xlike.top.kn_ai_chat.service.NotionService;
import xlike.top.kn_ai_chat.tools.dto.NotionResponse;
import xlike.top.kn_ai_chat.tools.dto.OperationRequest;

/**
 * 创建页面的具体操作类，实现了NotionOperation接口。
 * @author Administrator
 */
@RequiredArgsConstructor
public class CreatePageOperation implements NotionOperation<NotionResponse.Page> {

    private final OperationRequest.CreatePageRequestDto request;

    @Override
    public NotionResponse.Page execute(NotionService notionService) {
        return notionService.createPage(
                request.getTitle(),
                request.getTitlePropertyName(),
                request.getContent()
        );
    }
}