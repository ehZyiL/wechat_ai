package xlike.top.kn_ai_chat.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.handler.MessageHandler;
import xlike.top.kn_ai_chat.reply.Reply;
import xlike.top.kn_ai_chat.service.WeChatUserService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 消息分发器.
 * 负责根据消息内容和处理器优先级，将消息路由到合适的MessageHandler进行处理.
 * 同时，在此处实现拉黑功能的前置检查.
 * @author xlike
 */
@Service
public class MessageDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);
    private final List<MessageHandler> messageHandlers;
    private final WeChatUserService weChatUserService;

    public MessageDispatcher(List<MessageHandler> messageHandlers, WeChatUserService weChatUserService) {
        messageHandlers.sort(Comparator.comparingInt(MessageHandler::getOrder));
        this.messageHandlers = messageHandlers;
        this.weChatUserService = weChatUserService;
    }

    /**
     * 分发消息到对应的处理器.
     * 在分发前会检查用户是否被拉黑.
     * @param externalUserId 外部用户ID
     * @param openKfid 客服ID
     * @param content 消息内容
     * @param history 历史消息记录
     * @return 返回包含Reply对象的Optional；若不处理或用户被拉黑则返回 empty
     */
    public Optional<Reply> dispatch(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        // 在分发任何消息前，检查用户是否被拉黑
        if (weChatUserService.isUserBlocked(externalUserId)) {
            logger.info("用户 [{}] 已被拉黑，拒绝回复。", externalUserId);
            return Optional.empty();
        }

        for (MessageHandler handler : messageHandlers) {
            if (handler.canHandle(content)) {
                return handler.handle(externalUserId, openKfid, content, history);
            }
        }
        return Optional.empty();
    }
}