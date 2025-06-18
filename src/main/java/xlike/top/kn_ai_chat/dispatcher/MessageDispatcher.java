package xlike.top.kn_ai_chat.dispatcher;

import org.springframework.stereotype.Service;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.handler.MessageHandler;
import xlike.top.kn_ai_chat.reply.Reply;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
@Service
public class MessageDispatcher {

    private final List<MessageHandler> messageHandlers;

    /**
     * Spring会自动注入所有实现了MessageHandler接口的Bean
     * @param messageHandlers 处理器列表
     */
    public MessageDispatcher(List<MessageHandler> messageHandlers) {
        messageHandlers.sort(Comparator.comparingInt(MessageHandler::getOrder));
        this.messageHandlers = messageHandlers;
    }

    /**
     * 分发消息到合适的处理器
     * @param externalUserId 外部用户ID
     * @param openKfid 客服ID
     * @param content 消息内容
     * @param history 历史消息记录
     * @return 返回处理后的回复文本
     */
    public Optional<Reply> dispatch(String externalUserId, String openKfid, String content, List<MessageLog> history) {
        for (MessageHandler handler : messageHandlers) {
            if (handler.canHandle(content)) {
                return handler.handle(externalUserId, openKfid, content, history);
            }
        }
        return Optional.empty();
    }
}