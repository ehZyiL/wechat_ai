package xlike.top.kn_ai_chat.handler;

import org.springframework.core.Ordered;
import xlike.top.kn_ai_chat.domain.MessageLog;
import xlike.top.kn_ai_chat.reply.Reply; // <-- 导入Reply

import java.util.List;
import java.util.Optional;

/**
 * 消息处理器接口
 * 定义了处理微信消息的统一规范
 * 继承 Ordered 接口以支持排序
 * @author Administrator
 */
public interface MessageHandler extends Ordered {

    /**
     * 判断当前处理器是否能够处理这条消息
     * @param content 消息内容
     * @return 如果能处理，返回 true，否则 false
     */
    boolean canHandle(String content);

    /**
     * 处理消息并返回一个 Reply 对象
     * @param externalUserId 外部用户ID
     * @param openKfid 客服ID
     * @param content 消息内容
     * @param history 历史消息记录
     * @return 返回包含 Reply 对象的 Optional；若不处理则返回 empty
     */
    Optional<Reply> handle(String externalUserId, String openKfid, String content, List<MessageLog> history);

    /**
     * getOrder 方法来自 Ordered 接口，用于定义执行顺序
     * 数字越小，优先级越高
     */
    @Override
    int getOrder();
}