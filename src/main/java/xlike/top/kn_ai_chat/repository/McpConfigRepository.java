package xlike.top.kn_ai_chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xlike.top.kn_ai_chat.domain.McpConfig;

import java.util.Optional;

/**
 * @author xlike
 */
public interface McpConfigRepository extends JpaRepository<McpConfig, Long> {
    // 根据名称查找，用于防止重复加载
    Optional<McpConfig> findByName(String name);
}