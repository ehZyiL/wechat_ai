package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.KeywordConfig;

import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 */
public interface KeywordConfigRepository extends JpaRepository<KeywordConfig, Long> {
    List<KeywordConfig> findByExternalUserId(String externalUserId);
    Optional<KeywordConfig> findByExternalUserIdAndHandlerName(String externalUserId, String handlerName);
}