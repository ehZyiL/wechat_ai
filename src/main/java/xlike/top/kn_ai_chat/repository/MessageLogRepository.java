package xlike.top.kn_ai_chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.MessageLog;

import java.util.List;

/**
 * @author xlike
 */
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    List<MessageLog> findByFromUserOrToUserOrderByTimestampDesc(String userId1, String userId2, Pageable pageable);

    boolean existsByMsgId(String msgId);
    
    @Transactional
    void deleteByFromUserOrToUser(String fromUser, String toUser);

    long countByFromUserOrToUser(String fromUser, String toUser);

    /**
     * 根据发送者ID，按时间倒序查询其发送的消息（分页）
     * @param fromUser 发送者ID
     * @param pageable 分页参数，用于限制查询数量
     * @return 消息记录列表
     */
    List<MessageLog> findByFromUserOrderByTimestampDesc(String fromUser, Pageable pageable);
}