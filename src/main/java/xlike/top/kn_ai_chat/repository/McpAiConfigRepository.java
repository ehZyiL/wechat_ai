package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.McpAiConfig;

import java.util.Optional;

/**
 * @author xlike
 */
public interface McpAiConfigRepository extends JpaRepository<McpAiConfig, String> {
    Optional<McpAiConfig> findByExternalUserId(String externalUserId);
}