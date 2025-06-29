package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.ChatMessage;

import java.util.List;

/**
 * @author xlike
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 查找两个用户之间的所有聊天记录
     */
    List<ChatMessage> findByExternalUserIdOrderByTimestampAsc(String userId);
}