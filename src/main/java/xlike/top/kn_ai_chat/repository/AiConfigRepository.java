package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.AiConfig;

import java.util.Optional;

/**
 * @author Administrator
 */
public interface AiConfigRepository extends JpaRepository<AiConfig, String> {
    Optional<AiConfig> findByExternalUserId(String externalUserId);
}