package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import xlike.top.kn_ai_chat.domain.AiConfig;

import java.util.Optional;

/**
 * @author xlike
 */
public interface AiConfigRepository extends JpaRepository<AiConfig, String> {

    Optional<AiConfig> findByExternalUserId(String externalUserId);

    /**
     * 根据用户ID删除其AI配置
     * @param externalUserId 用户的 externalUserId
     */
    @Transactional
    void deleteByExternalUserId(String externalUserId);
}