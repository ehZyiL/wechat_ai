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

    public Optional<Reply> dispatch(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        if (weChatUserService.isUserBlocked(externalUserId)) {
            logger.info("用户 [{}] 已被拉黑，拒绝回复。", externalUserId);
            return Optional.empty();
        }

        for (MessageHandler handler : messageHandlers) {
            if (handler.canHandle(content, externalUserId)) {
                return handler.handle(externalUserId, openKfid, content, history);
            }
        }
        return Optional.empty();
    }
}