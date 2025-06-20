package xlike.top.kn_ai_chat.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.reply.TextReply;
import xlike.top.kn_ai_chat.service.CustomReplyService;

import java.util.List;
import java.util.Optional;

/**
 * 自定义关键词回复处理器
 * 这是最高优先级的处理器
 * @author Administrator
 */
@Component
@RequiredArgsConstructor
public class CustomReplyHandler implements MessageHandler {

    private final CustomReplyService customReplyService;

    /**
     * 设置最高优先级
     * 返回一个非常小的值，确保在 MessageDispatcher 的排序中排在最前面
     * @return 优先级顺序
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 判断是否能处理此消息
     * 如果 CustomReplyService 能根据用户的输入找到对应的回复，则可以处理
     * @param content 消息内容
     * @param externalUserId 用户ID
     * @return boolean
     */
    @Override
    public boolean canHandle(String content, String externalUserId) {
        // 直接查找是否存在对应的回复
        return customReplyService.findReplyForKeyword(content, externalUserId).isPresent();
    }

    /**
     * 处理消息
     * canHandle 返回 true 后，此方法将被调用
     * @param externalUserId 用户ID
     * @param openKfid ...
     * @param content 消息内容
     * @param history 历史消息
     * @return 包装在Optional中的Reply对象
     */
    @Override
    public Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        // 再次查找（因为 canHandle 的结果不会传递过来，这里做一次重复查找是必要的）
        // 然后将找到的文本包装成 TextReply 并返回
        return customReplyService.findReplyForKeyword(content, externalUserId)
                .map(TextReply::new);
    }
}