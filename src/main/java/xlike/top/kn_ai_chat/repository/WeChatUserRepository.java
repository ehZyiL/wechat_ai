package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.WeChatUser;

/**
 * @author Administrator
 */
public interface WeChatUserRepository extends JpaRepository<WeChatUser, String> {
}